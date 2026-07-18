package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.DivergenciaResposta;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.dto.ReconciliacaoBaplieExecucaoDTO.ReconciliacaoResposta;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.DecisaoResolucao;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.FonteDado;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.SeveridadeDivergencia;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao.TipoDivergencia;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.ReconciliacaoBaplieExecucao;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio.DivergenciaReconciliacaoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio.ReconciliacaoBaplieExecucaoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliacaoBaplieExecucaoServico {

    private static final double TOLERANCIA_PESO_MINIMA_KG = 500.0;
    private static final double TOLERANCIA_PESO_PERCENTUAL = 0.02;
    private static final double LIMITE_CRITICO_PESO_MINIMO_KG = 1000.0;
    private static final double LIMITE_CRITICO_PESO_PERCENTUAL = 0.05;

    private final EstivagemPlanRepositorio planoRepositorio;
    private final BayPlanRepositorio bayPlanRepositorio;
    private final UnidadeInventarioRepositorio inventarioRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final ReconciliacaoBaplieExecucaoRepositorio reconciliacaoRepositorio;
    private final DivergenciaReconciliacaoRepositorio divergenciaRepositorio;

    public ReconciliacaoBaplieExecucaoServico(
            EstivagemPlanRepositorio planoRepositorio,
            BayPlanRepositorio bayPlanRepositorio,
            UnidadeInventarioRepositorio inventarioRepositorio,
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            ReconciliacaoBaplieExecucaoRepositorio reconciliacaoRepositorio,
            DivergenciaReconciliacaoRepositorio divergenciaRepositorio) {
        this.planoRepositorio = planoRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.reconciliacaoRepositorio = reconciliacaoRepositorio;
        this.divergenciaRepositorio = divergenciaRepositorio;
    }

    @Transactional
    public ReconciliacaoResposta reconciliar(Long planoId, String usuario) {
        EstivagemPlan plano = buscarPlano(planoId);
        BayPlan bayPlan = bayPlanRepositorio.findById(plano.getBayPlanId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "BayPlan não encontrado: " + plano.getBayPlanId()));

        Map<String, BayPlanContainer> baplie = indexarBaplie(bayPlan.getContainers());
        Map<String, SlotNavio> slots = indexarPlano(plano.getSlots());
        Map<String, UnidadeInventario> inventario = indexarInventario(
                inventarioRepositorio.findAllByOrderByIdentificacaoAsc());
        Map<String, OrdemTrabalhoPatio> execucao = indexarExecucao(plano.getVisitaNavioId());

        Set<String> unidadesRelacionadas = new TreeSet<>();
        unidadesRelacionadas.addAll(baplie.keySet());
        unidadesRelacionadas.addAll(slots.keySet());
        unidadesRelacionadas.addAll(execucao.keySet());

        ReconciliacaoBaplieExecucao reconciliacao = new ReconciliacaoBaplieExecucao();
        reconciliacao.setPlano(plano);
        reconciliacao.setBayPlanId(bayPlan.getId());
        reconciliacao.setVisitaNavioId(plano.getVisitaNavioId());
        reconciliacao.setVersaoPlano(plano.getVersao());
        reconciliacao.setSolicitante(normalizarUsuario(usuario));
        reconciliacao.setTotalUnidades(unidadesRelacionadas.size());

        for (String codigo : unidadesRelacionadas) {
            reconciliarUnidade(
                    reconciliacao,
                    codigo,
                    baplie.get(codigo),
                    slots.get(codigo),
                    inventario.get(codigo),
                    execucao.get(codigo));
        }

        reconciliacao.recalcularTotais();
        return mapear(reconciliacaoRepositorio.save(reconciliacao));
    }

    @Transactional(readOnly = true)
    public ReconciliacaoResposta buscarAtual(Long planoId) {
        buscarPlano(planoId);
        return reconciliacaoRepositorio.findTopByPlanoIdOrderByExecutadaEmDesc(planoId)
                .map(this::mapear)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhuma reconciliação encontrada para o plano " + planoId));
    }

    @Transactional
    public ReconciliacaoResposta resolverDivergencia(
            Long planoId,
            Long reconciliacaoId,
            Long divergenciaId,
            DecisaoResolucao decisao,
            String motivo,
            String usuario) {
        ReconciliacaoBaplieExecucao reconciliacao = reconciliacaoRepositorio.findById(reconciliacaoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reconciliação não encontrada: " + reconciliacaoId));
        if (!Objects.equals(reconciliacao.getPlano().getId(), planoId)) {
            throw new IllegalArgumentException("A reconciliação não pertence ao plano informado");
        }

        DivergenciaReconciliacao divergencia = divergenciaRepositorio
                .findByIdAndReconciliacaoId(divergenciaId, reconciliacaoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Divergência não encontrada: " + divergenciaId));
        divergencia.resolver(decisao, limitar(motivo, 1000), normalizarUsuario(usuario));
        divergenciaRepositorio.save(divergencia);

        reconciliacao.recalcularTotais();
        reconciliacaoRepositorio.save(reconciliacao);
        return mapear(reconciliacao);
    }

    @Transactional(readOnly = true)
    public void exigirSemDivergenciasCriticas(Long planoId) {
        EstivagemPlan plano = buscarPlano(planoId);
        ReconciliacaoBaplieExecucao atual = reconciliacaoRepositorio
                .findTopByPlanoIdOrderByExecutadaEmDesc(planoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Execute a reconciliação BAPLIE antes de publicar ou concluir o plano"));

        if (!Objects.equals(atual.getVersaoPlano(), plano.getVersao())) {
            throw new IllegalStateException(
                    "A reconciliação está desatualizada porque o plano foi alterado");
        }
        atual.recalcularTotais();
        if (atual.bloqueiaOperacao()) {
            throw new IllegalStateException(
                    "O plano possui " + atual.getTotalCriticasAbertas()
                            + " divergência(s) crítica(s) não resolvida(s)");
        }
    }

    private void reconciliarUnidade(
            ReconciliacaoBaplieExecucao reconciliacao,
            String codigo,
            BayPlanContainer baplie,
            SlotNavio slot,
            UnidadeInventario inventario,
            OrdemTrabalhoPatio execucao) {
        if (baplie != null && slot == null) {
            adicionar(reconciliacao, null, codigo, TipoDivergencia.UNIDADE,
                    SeveridadeDivergencia.CRITICA, "unidade",
                    FonteDado.BAPLIE, codigo,
                    FonteDado.PLANO_APROVADO, null);
        } else if (baplie == null && slot != null) {
            adicionar(reconciliacao, slot, codigo, TipoDivergencia.UNIDADE,
                    SeveridadeDivergencia.CRITICA, "unidade",
                    FonteDado.PLANO_APROVADO, codigo,
                    FonteDado.BAPLIE, null);
        } else if (baplie == null && slot == null && execucao != null) {
            adicionar(reconciliacao, null, codigo, TipoDivergencia.EXECUCAO,
                    SeveridadeDivergencia.CRITICA, "unidadeExecutada",
                    FonteDado.EXECUCAO, execucao.getStatusOrdem().name(),
                    FonteDado.PLANO_APROVADO, null);
        }

        if (inventario == null) {
            adicionar(reconciliacao, slot, codigo, TipoDivergencia.UNIDADE,
                    SeveridadeDivergencia.CRITICA, "unidadeInventario",
                    slot != null ? FonteDado.PLANO_APROVADO : FonteDado.BAPLIE,
                    codigo,
                    FonteDado.INVENTARIO,
                    null);
        }

        if (baplie != null && slot != null) {
            compararTexto(reconciliacao, slot, codigo, TipoDivergencia.SLOT,
                    SeveridadeDivergencia.CRITICA, "slot",
                    FonteDado.BAPLIE, posicaoBaplie(baplie),
                    FonteDado.PLANO_APROVADO, posicaoSlot(slot));
            compararPeso(reconciliacao, slot, codigo,
                    FonteDado.BAPLIE, baplie.getPesoOperacionalKg(),
                    FonteDado.PLANO_APROVADO, pesoPlano(slot));
            compararTexto(reconciliacao, slot, codigo, TipoDivergencia.PORTO,
                    SeveridadeDivergencia.CRITICA, "portoDescarga",
                    FonteDado.BAPLIE, baplie.getPortoDescarga(),
                    FonteDado.PLANO_APROVADO, slot.getPortoDescarga());
            compararBooleano(reconciliacao, slot, codigo, TipoDivergencia.PERIGOSO,
                    "perigoso", FonteDado.BAPLIE, baplie.isPerigoso(),
                    FonteDado.PLANO_APROVADO, slot.isPerigoso());
            compararBooleano(reconciliacao, slot, codigo, TipoDivergencia.REEFER,
                    "reefer", FonteDado.BAPLIE, baplie.isReefer(),
                    FonteDado.PLANO_APROVADO, slot.isReefer());
        }

        if (slot != null && inventario != null) {
            compararPeso(reconciliacao, slot, codigo,
                    FonteDado.PLANO_APROVADO, pesoPlano(slot),
                    FonteDado.INVENTARIO, pesoInventario(inventario));
            reconciliarPosicaoFisica(reconciliacao, codigo, slot, inventario);
        }

        if (execucao != null) {
            reconciliarExecucao(reconciliacao, codigo, slot, execucao);
        }
    }

    private void reconciliarPosicaoFisica(
            ReconciliacaoBaplieExecucao reconciliacao,
            String codigo,
            SlotNavio slot,
            UnidadeInventario inventario) {
        if (inventario.getEstado() != UnidadeInventario.EstadoUnidade.EMBARCADA) {
            return;
        }
        String posicaoFisica = normalizarPosicaoNavio(inventario.getPosicaoAtual());
        compararTexto(reconciliacao, slot, codigo, TipoDivergencia.POSICAO_FISICA,
                SeveridadeDivergencia.CRITICA, "posicaoFisica",
                FonteDado.PLANO_APROVADO, posicaoSlot(slot),
                FonteDado.POSICAO_FISICA, posicaoFisica);
    }

    private void reconciliarExecucao(
            ReconciliacaoBaplieExecucao reconciliacao,
            String codigo,
            SlotNavio slot,
            OrdemTrabalhoPatio execucao) {
        if (execucao.getStatusOrdem() == StatusOrdemTrabalhoPatio.BLOQUEADA
                || execucao.getStatusOrdem() == StatusOrdemTrabalhoPatio.SUSPENSA) {
            adicionar(reconciliacao, slot, codigo, TipoDivergencia.EXECUCAO,
                    SeveridadeDivergencia.ALERTA, "statusExecucao",
                    FonteDado.PLANO_APROVADO, "OPERACIONAL",
                    FonteDado.EXECUCAO, execucao.getStatusOrdem().name());
        }
        if (slot == null || execucao.getStatusOrdem() != StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        String destinoExecutado = normalizarPosicaoNavio(execucao.getDestino());
        if (destinoExecutado != null) {
            compararTexto(reconciliacao, slot, codigo, TipoDivergencia.EXECUCAO,
                    SeveridadeDivergencia.CRITICA, "slotExecutado",
                    FonteDado.PLANO_APROVADO, posicaoSlot(slot),
                    FonteDado.EXECUCAO, destinoExecutado);
        }
    }

    private void compararPeso(
            ReconciliacaoBaplieExecucao reconciliacao,
            SlotNavio slot,
            String codigo,
            FonteDado fonteReferencia,
            Double referencia,
            FonteDado fonteDivergente,
            Double divergente) {
        if (referencia == null || divergente == null) {
            return;
        }
        double diferenca = Math.abs(referencia - divergente);
        double tolerancia = Math.max(
                TOLERANCIA_PESO_MINIMA_KG,
                Math.abs(referencia) * TOLERANCIA_PESO_PERCENTUAL);
        if (diferenca <= tolerancia) {
            return;
        }
        double limiteCritico = Math.max(
                LIMITE_CRITICO_PESO_MINIMO_KG,
                Math.abs(referencia) * LIMITE_CRITICO_PESO_PERCENTUAL);
        SeveridadeDivergencia severidade = diferenca > limiteCritico
                ? SeveridadeDivergencia.CRITICA
                : SeveridadeDivergencia.ALERTA;
        adicionar(reconciliacao, slot, codigo, TipoDivergencia.PESO, severidade,
                "pesoKg", fonteReferencia, formatarNumero(referencia),
                fonteDivergente, formatarNumero(divergente));
    }

    private void compararTexto(
            ReconciliacaoBaplieExecucao reconciliacao,
            SlotNavio slot,
            String codigo,
            TipoDivergencia tipo,
            SeveridadeDivergencia severidade,
            String campo,
            FonteDado fonteReferencia,
            String referencia,
            FonteDado fonteDivergente,
            String divergente) {
        if (Objects.equals(normalizarTexto(referencia), normalizarTexto(divergente))) {
            return;
        }
        adicionar(reconciliacao, slot, codigo, tipo, severidade, campo,
                fonteReferencia, referencia, fonteDivergente, divergente);
    }

    private void compararBooleano(
            ReconciliacaoBaplieExecucao reconciliacao,
            SlotNavio slot,
            String codigo,
            TipoDivergencia tipo,
            String campo,
            FonteDado fonteReferencia,
            boolean referencia,
            FonteDado fonteDivergente,
            boolean divergente) {
        if (referencia == divergente) {
            return;
        }
        adicionar(reconciliacao, slot, codigo, tipo, SeveridadeDivergencia.CRITICA,
                campo, fonteReferencia, Boolean.toString(referencia),
                fonteDivergente, Boolean.toString(divergente));
    }

    private void adicionar(
            ReconciliacaoBaplieExecucao reconciliacao,
            SlotNavio slot,
            String codigo,
            TipoDivergencia tipo,
            SeveridadeDivergencia severidade,
            String campo,
            FonteDado fonteReferencia,
            String valorReferencia,
            FonteDado fonteDivergente,
            String valorDivergente) {
        DivergenciaReconciliacao divergencia = new DivergenciaReconciliacao();
        divergencia.setSlotNavio(slot);
        divergencia.setCodigoContainer(codigo);
        divergencia.setTipo(tipo);
        divergencia.setSeveridade(severidade);
        divergencia.setCampo(campo);
        divergencia.setFonteReferencia(fonteReferencia);
        divergencia.setValorReferencia(limitar(valorReferencia, 4000));
        divergencia.setFonteDivergente(fonteDivergente);
        divergencia.setValorDivergente(limitar(valorDivergente, 4000));
        reconciliacao.adicionarDivergencia(divergencia);
    }

    private Map<String, BayPlanContainer> indexarBaplie(List<BayPlanContainer> containers) {
        Map<String, BayPlanContainer> resultado = new LinkedHashMap<>();
        if (containers != null) {
            for (BayPlanContainer container : containers) {
                if (container != null && container.getCodigoContainer() != null) {
                    resultado.put(chave(container.getCodigoContainer()), container);
                }
            }
        }
        return resultado;
    }

    private Map<String, SlotNavio> indexarPlano(List<SlotNavio> slots) {
        Map<String, SlotNavio> resultado = new LinkedHashMap<>();
        if (slots != null) {
            for (SlotNavio slot : slots) {
                if (slot != null && slot.getCodigoContainer() != null) {
                    resultado.put(chave(slot.getCodigoContainer()), slot);
                }
            }
        }
        return resultado;
    }

    private Map<String, UnidadeInventario> indexarInventario(List<UnidadeInventario> unidades) {
        Map<String, UnidadeInventario> resultado = new LinkedHashMap<>();
        if (unidades != null) {
            for (UnidadeInventario unidade : unidades) {
                if (unidade != null && unidade.getIdentificacao() != null) {
                    resultado.put(chave(unidade.getIdentificacao()), unidade);
                }
            }
        }
        return resultado;
    }

    private Map<String, OrdemTrabalhoPatio> indexarExecucao(Long visitaNavioId) {
        Map<String, OrdemTrabalhoPatio> resultado = new LinkedHashMap<>();
        if (visitaNavioId == null) {
            return resultado;
        }
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId);
        for (OrdemTrabalhoPatio ordem : ordens) {
            if (ordem != null && ordem.getCodigoConteiner() != null) {
                resultado.put(chave(ordem.getCodigoConteiner()), ordem);
            }
        }
        return resultado;
    }

    private String posicaoBaplie(BayPlanContainer container) {
        PosicaoBay posicao = container.getPosicaoBay();
        return posicao == null ? null : formatarPosicao(
                posicao.getBay(), posicao.getRow(), posicao.getTier());
    }

    private String posicaoSlot(SlotNavio slot) {
        return formatarPosicao(slot.getBay(), slot.getRowBay(), slot.getTier());
    }

    private String formatarPosicao(Integer bay, Integer row, Integer tier) {
        if (bay == null || row == null || tier == null) {
            return null;
        }
        return String.format(Locale.ROOT, "%02d%02d%02d", bay, row, tier);
    }

    private String normalizarPosicaoNavio(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        boolean contextoNavio = normalizado.matches(".*(NAVIO|BAY|SLOT).*");
        boolean somenteCodigo = normalizado.matches("\\d{6,7}");
        if (!contextoNavio && !somenteCodigo) {
            return null;
        }
        String digitos = normalizado.replaceAll("\\D", "");
        if (digitos.length() == 6) {
            return digitos;
        }
        if (digitos.length() == 7) {
            return formatarPosicao(
                    Integer.parseInt(digitos.substring(0, 3)),
                    Integer.parseInt(digitos.substring(3, 5)),
                    Integer.parseInt(digitos.substring(5, 7)));
        }
        return null;
    }

    private Double pesoPlano(SlotNavio slot) {
        return slot.getPesoVgmKg() != null ? slot.getPesoVgmKg() : slot.getPesoKg();
    }

    private Double pesoInventario(UnidadeInventario unidade) {
        BigDecimal peso = unidade.getPesoBrutoKg();
        return peso == null ? null : peso.doubleValue();
    }

    private EstivagemPlan buscarPlano(Long planoId) {
        return planoRepositorio.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "EstivagemPlan não encontrado: " + planoId));
    }

    private String chave(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String formatarNumero(Double valor) {
        return valor == null ? null : String.format(Locale.ROOT, "%.3f", valor);
    }

    private String normalizarUsuario(String usuario) {
        String normalizado = usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
        return limitar(normalizado, 120);
    }

    private String limitar(String valor, int limite) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.length() <= limite
                ? normalizado
                : normalizado.substring(0, limite);
    }

    private ReconciliacaoResposta mapear(ReconciliacaoBaplieExecucao reconciliacao) {
        reconciliacao.recalcularTotais();
        List<DivergenciaResposta> divergencias = new ArrayList<>();
        for (DivergenciaReconciliacao divergencia : reconciliacao.getDivergencias()) {
            divergencias.add(new DivergenciaResposta(
                    divergencia.getId(),
                    divergencia.getSlotNavio() == null ? null : divergencia.getSlotNavio().getId(),
                    divergencia.getCodigoContainer(),
                    nome(divergencia.getTipo()),
                    nome(divergencia.getSeveridade()),
                    nome(divergencia.getStatus()),
                    divergencia.getCampo(),
                    nome(divergencia.getFonteReferencia()),
                    divergencia.getValorReferencia(),
                    nome(divergencia.getFonteDivergente()),
                    divergencia.getValorDivergente(),
                    nome(divergencia.getDecisaoResolucao()),
                    divergencia.getMotivoResolucao(),
                    divergencia.getResponsavelResolucao(),
                    divergencia.getDetectadaEm(),
                    divergencia.getResolvidaEm()));
        }
        return new ReconciliacaoResposta(
                reconciliacao.getId(),
                reconciliacao.getPlano().getId(),
                reconciliacao.getBayPlanId(),
                reconciliacao.getVisitaNavioId(),
                reconciliacao.getVersaoPlano(),
                nome(reconciliacao.getStatus()),
                reconciliacao.getTotalUnidades(),
                reconciliacao.getTotalDivergencias(),
                reconciliacao.getTotalCriticasAbertas(),
                reconciliacao.bloqueiaOperacao(),
                reconciliacao.getSolicitante(),
                reconciliacao.getExecutadaEm(),
                reconciliacao.getConcluidaEm(),
                divergencias);
    }

    private String nome(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}
