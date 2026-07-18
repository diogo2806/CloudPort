package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.PosicaoBay;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.dto.DivergenciaReconciliacaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ReconciliacaoBaplieExecucaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ResolverDivergenciaRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.DecisaoResolucaoReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.DivergenciaReconciliacaoSlot;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SeveridadeDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusReconciliacaoSlot;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.DivergenciaReconciliacaoSlotRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReconciliacaoBaplieExecucaoServico {

    private static final double PESO_AVISO_KG = 100.0;
    private static final double PESO_CRITICO_KG = 1000.0;

    private final EstivagemPlanRepositorio planRepositorio;
    private final br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio bayPlanRepositorio;
    private final UnidadeInventarioRepositorio inventarioRepositorio;
    private final DivergenciaReconciliacaoSlotRepositorio divergenciaRepositorio;
    private final SlotNavioRepositorio slotRepositorio;

    public ReconciliacaoBaplieExecucaoServico(
            EstivagemPlanRepositorio planRepositorio,
            br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio bayPlanRepositorio,
            UnidadeInventarioRepositorio inventarioRepositorio,
            DivergenciaReconciliacaoSlotRepositorio divergenciaRepositorio,
            SlotNavioRepositorio slotRepositorio) {
        this.planRepositorio = planRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
        this.divergenciaRepositorio = divergenciaRepositorio;
        this.slotRepositorio = slotRepositorio;
    }

    @Transactional
    public ReconciliacaoBaplieExecucaoDto reconciliar(Long planoId) {
        return executar(plano(planoId));
    }

    @Transactional
    public void reconciliarPorBayPlan(Long bayPlanId) {
        if (bayPlanId != null) {
            planRepositorio.findByBayPlanId(bayPlanId).ifPresent(this::executar);
        }
    }

    @Transactional(readOnly = true)
    public ReconciliacaoBaplieExecucaoDto consultar(Long planoId) {
        EstivagemPlan plano = plano(planoId);
        return resposta(plano, divergenciaRepositorio.findByEstivagemIdOrderByCriadoEmAsc(planoId), LocalDateTime.now());
    }

    @Transactional
    public ReconciliacaoBaplieExecucaoDto resolver(
            Long planoId,
            Long divergenciaId,
            ResolverDivergenciaRequisicaoDto requisicao) {
        EstivagemPlan plano = plano(planoId);
        DivergenciaReconciliacaoSlot divergencia = divergenciaRepositorio
                .findByIdAndEstivagemId(divergenciaId, planoId)
                .orElseThrow(() -> new EntityNotFoundException("Divergência não encontrada: " + divergenciaId));
        if (divergencia.getStatus() == StatusDivergenciaReconciliacao.RESOLVIDA) {
            throw new IllegalStateException("A divergência já está resolvida.");
        }
        if (requisicao.getDecisao() == DecisaoResolucaoReconciliacao.RESOLVIDA_AUTOMATICAMENTE) {
            throw new IllegalArgumentException("Decisão reservada ao sistema.");
        }
        divergencia.setStatus(StatusDivergenciaReconciliacao.RESOLVIDA);
        divergencia.setDecisao(requisicao.getDecisao());
        divergencia.setJustificativa(requisicao.getJustificativa().trim());
        divergencia.setResolvidoPor(usuario());
        divergencia.setResolvidoEm(LocalDateTime.now());
        divergenciaRepositorio.save(divergencia);
        List<DivergenciaReconciliacaoSlot> itens = divergenciaRepositorio.findByEstivagemIdOrderByCriadoEmAsc(planoId);
        atualizarResumo(plano, itens, LocalDateTime.now());
        atualizarConclusao(plano, itens);
        return resposta(plano, itens, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public void exigirSemDivergenciasCriticas(Long planoId) {
        long total = divergenciaRepositorio.countByEstivagemIdAndSeveridadeAndStatus(
                planoId,
                SeveridadeDivergenciaReconciliacao.CRITICA,
                StatusDivergenciaReconciliacao.ABERTA);
        if (total > 0) {
            throw new IllegalStateException("Plano possui " + total
                    + " divergência(s) crítica(s) de reconciliação aberta(s) e não pode ser publicado ou concluído.");
        }
    }

    private ReconciliacaoBaplieExecucaoDto executar(EstivagemPlan plano) {
        BayPlan bayPlan = bayPlanRepositorio.findById(plano.getBayPlanId())
                .orElseThrow(() -> new EntityNotFoundException("Bay Plan não encontrado: " + plano.getBayPlanId()));
        Map<String, BayPlanContainer> baplie = containers(bayPlan);
        Map<String, SlotNavio> slots = slots(plano);
        List<Detectada> detectadas = detectar(baplie, slots);
        Map<String, DivergenciaReconciliacaoSlot> persistidas = divergenciaRepositorio
                .findByEstivagemIdOrderByCriadoEmAsc(plano.getId()).stream()
                .collect(Collectors.toMap(
                        DivergenciaReconciliacaoSlot::getChave,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new));
        Set<String> atuais = detectadas.stream().map(Detectada::chave).collect(Collectors.toSet());
        detectadas.forEach(item -> aplicar(plano, persistidas, item));
        LocalDateTime agora = LocalDateTime.now();
        persistidas.values().stream()
                .filter(item -> !atuais.contains(item.getChave()))
                .filter(item -> item.getStatus() == StatusDivergenciaReconciliacao.ABERTA)
                .forEach(item -> resolverAutomaticamente(item, agora));
        List<DivergenciaReconciliacaoSlot> salvas = divergenciaRepositorio.saveAll(new ArrayList<>(persistidas.values()));
        atualizarResumo(plano, salvas, agora);
        atualizarConclusao(plano, salvas);
        return resposta(plano, salvas, agora);
    }

    private List<Detectada> detectar(
            Map<String, BayPlanContainer> baplie,
            Map<String, SlotNavio> slots) {
        List<Detectada> resultado = new ArrayList<>();
        for (Map.Entry<String, BayPlanContainer> entry : baplie.entrySet()) {
            String codigo = entry.getKey();
            BayPlanContainer container = entry.getValue();
            SlotNavio slot = slots.get(codigo);
            UnidadeInventario inventario = inventarioRepositorio
                    .findByIdentificacaoIgnoreCase(codigo).orElse(null);
            String b = snapshotBaplie(container);
            String p = snapshotPlano(slot);
            String i = snapshotInventario(inventario);
            String e = snapshotExecucao(container);
            if (slot == null) {
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.AUSENTE_NO_PLANO,
                        SeveridadeDivergenciaReconciliacao.CRITICA, null, b, p, i, e));
                continue;
            }
            if (diferente(posicao(container.getPosicaoBay()), posicao(slot))) {
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.POSICAO_PLANO_DIVERGENTE,
                        SeveridadeDivergenciaReconciliacao.CRITICA, slot, b, p, i, e));
            }
            if (StringUtils.hasText(container.getIsoCode()) && StringUtils.hasText(slot.getIsoCode())
                    && !container.getIsoCode().equalsIgnoreCase(slot.getIsoCode())) {
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.ISO_DIVERGENTE,
                        SeveridadeDivergenciaReconciliacao.CRITICA, slot, b, p, i, e));
            }
            if (inventario == null) {
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.AUSENTE_NO_INVENTARIO,
                        SeveridadeDivergenciaReconciliacao.AVISO, slot, b, p, i, e));
            } else {
                avaliarInventario(resultado, codigo, slot, inventario, b, p, i, e);
            }
            avaliarExecucao(resultado, codigo, container, slot, b, p, i, e);
            avaliarPeso(resultado, codigo, container, slot, inventario, b, p, i, e);
        }
        slots.forEach((codigo, slot) -> {
            if (!baplie.containsKey(codigo)) {
                UnidadeInventario inventario = inventarioRepositorio
                        .findByIdentificacaoIgnoreCase(codigo).orElse(null);
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.AUSENTE_NO_BAPLIE,
                        SeveridadeDivergenciaReconciliacao.CRITICA, slot,
                        null, snapshotPlano(slot), snapshotInventario(inventario), null));
            }
        });
        return resultado;
    }

    private void avaliarInventario(
            List<Detectada> resultado,
            String codigo,
            SlotNavio slot,
            UnidadeInventario inventario,
            String b,
            String p,
            String i,
            String e) {
        String informada = primeiraPosicao(inventario.getPosicaoPlanejada(), inventario.getPosicaoAtual());
        if (!StringUtils.hasText(informada)) {
            if (StringUtils.hasText(inventario.getPosicaoPlanejada()) || StringUtils.hasText(inventario.getPosicaoAtual())) {
                resultado.add(nova(codigo, TipoDivergenciaReconciliacao.POSICAO_INVENTARIO_NAO_COMPARAVEL,
                        SeveridadeDivergenciaReconciliacao.INFORMATIVA, slot, b, p, i, e));
            }
        } else if (!informada.equals(posicao(slot))) {
            resultado.add(nova(codigo, TipoDivergenciaReconciliacao.POSICAO_INVENTARIO_DIVERGENTE,
                    SeveridadeDivergenciaReconciliacao.CRITICA, slot, b, p, i, e));
        }
    }

    private void avaliarExecucao(
            List<Detectada> resultado,
            String codigo,
            BayPlanContainer container,
            SlotNavio slot,
            String b,
            String p,
            String i,
            String e) {
        if (!concluido(container)) {
            return;
        }
        String executada = posicao(container.getPosicaoExecucao());
        if (!StringUtils.hasText(executada)) {
            resultado.add(nova(codigo, TipoDivergenciaReconciliacao.EXECUCAO_SEM_POSICAO,
                    SeveridadeDivergenciaReconciliacao.CRITICA, slot, b, p, i, e));
        } else if (!executada.equals(posicao(slot))) {
            resultado.add(nova(codigo, TipoDivergenciaReconciliacao.POSICAO_EXECUCAO_DIVERGENTE,
                    SeveridadeDivergenciaReconciliacao.CRITICA, slot, b, p, i, e));
        }
    }

    private void avaliarPeso(
            List<Detectada> resultado,
            String codigo,
            BayPlanContainer container,
            SlotNavio slot,
            UnidadeInventario inventario,
            String b,
            String p,
            String i,
            String e) {
        List<Double> pesos = new ArrayList<>();
        peso(pesos, container.getPesoOperacionalKg());
        peso(pesos, slot.getPesoVgmKg() != null ? slot.getPesoVgmKg() : slot.getPesoKg());
        peso(pesos, inventario == null ? null : inventario.getPesoBrutoKg());
        peso(pesos, container.getPesoExecucaoKg());
        if (pesos.size() < 2) {
            return;
        }
        double diferenca = pesos.stream().max(Double::compareTo).orElse(0.0)
                - pesos.stream().min(Double::compareTo).orElse(0.0);
        if (diferenca > PESO_AVISO_KG) {
            SeveridadeDivergenciaReconciliacao severidade = diferenca > PESO_CRITICO_KG
                    ? SeveridadeDivergenciaReconciliacao.CRITICA
                    : SeveridadeDivergenciaReconciliacao.AVISO;
            resultado.add(nova(codigo, TipoDivergenciaReconciliacao.PESO_DIVERGENTE,
                    severidade, slot, b, p, i, e));
        }
    }

    private Detectada nova(
            String codigo,
            TipoDivergenciaReconciliacao tipo,
            SeveridadeDivergenciaReconciliacao severidade,
            SlotNavio slot,
            String baplie,
            String plano,
            String inventario,
            String execucao) {
        return new Detectada(codigo + "|" + tipo.name(), codigo, tipo, severidade, slot,
                baplie, plano, inventario, execucao, assinatura(baplie, plano, inventario, execucao));
    }

    private void aplicar(
            EstivagemPlan plano,
            Map<String, DivergenciaReconciliacaoSlot> persistidas,
            Detectada detectada) {
        DivergenciaReconciliacaoSlot item = persistidas.get(detectada.chave());
        if (item == null) {
            item = new DivergenciaReconciliacaoSlot();
            item.setEstivagem(plano);
            item.setChave(detectada.chave());
            item.setCodigoContainer(detectada.codigo());
            item.setStatus(StatusDivergenciaReconciliacao.ABERTA);
            persistidas.put(detectada.chave(), item);
        } else if (!Objects.equals(item.getAssinaturaFontes(), detectada.assinatura())
                || item.getDecisao() == DecisaoResolucaoReconciliacao.RESOLVIDA_AUTOMATICAMENTE) {
            item.setStatus(StatusDivergenciaReconciliacao.ABERTA);
            item.setDecisao(null);
            item.setJustificativa(null);
            item.setResolvidoPor(null);
            item.setResolvidoEm(null);
        }
        item.setSlot(detectada.slot());
        item.setTipo(detectada.tipo());
        item.setSeveridade(detectada.severidade());
        item.setValorBaplie(detectada.baplie());
        item.setValorPlano(detectada.plano());
        item.setValorInventario(detectada.inventario());
        item.setValorExecucao(detectada.execucao());
        item.setAssinaturaFontes(detectada.assinatura());
    }

    private void resolverAutomaticamente(DivergenciaReconciliacaoSlot item, LocalDateTime agora) {
        item.setStatus(StatusDivergenciaReconciliacao.RESOLVIDA);
        item.setDecisao(DecisaoResolucaoReconciliacao.RESOLVIDA_AUTOMATICAMENTE);
        item.setJustificativa("As fontes ficaram consistentes após nova reconciliação.");
        item.setResolvidoPor("SISTEMA");
        item.setResolvidoEm(agora);
    }

    private void atualizarResumo(
            EstivagemPlan plano,
            List<DivergenciaReconciliacaoSlot> divergencias,
            LocalDateTime agora) {
        for (SlotNavio slot : plano.getSlots()) {
            if (!StringUtils.hasText(slot.getCodigoContainer())) {
                slot.setStatusReconciliacao(StatusReconciliacaoSlot.NAO_RECONCILIADO);
                slot.setSeveridadeReconciliacao(null);
                slot.setReconciliadoEm(agora);
                continue;
            }
            List<DivergenciaReconciliacaoSlot> doSlot = divergencias.stream()
                    .filter(item -> item.getSlot() != null && Objects.equals(item.getSlot().getId(), slot.getId()))
                    .collect(Collectors.toList());
            List<DivergenciaReconciliacaoSlot> abertas = doSlot.stream()
                    .filter(item -> item.getStatus() == StatusDivergenciaReconciliacao.ABERTA)
                    .collect(Collectors.toList());
            slot.setStatusReconciliacao(!abertas.isEmpty()
                    ? StatusReconciliacaoSlot.DIVERGENTE
                    : doSlot.isEmpty() ? StatusReconciliacaoSlot.CONSISTENTE : StatusReconciliacaoSlot.RESOLVIDO);
            slot.setSeveridadeReconciliacao(maiorSeveridade(!abertas.isEmpty() ? abertas : doSlot));
            slot.setReconciliadoEm(agora);
        }
        slotRepositorio.saveAll(plano.getSlots());
    }

    private SeveridadeDivergenciaReconciliacao maiorSeveridade(List<DivergenciaReconciliacaoSlot> itens) {
        return itens.stream().map(DivergenciaReconciliacaoSlot::getSeveridade)
                .filter(Objects::nonNull).max(Comparator.comparingInt(Enum::ordinal)).orElse(null);
    }

    private void atualizarConclusao(EstivagemPlan plano, List<DivergenciaReconciliacaoSlot> divergencias) {
        BayPlan bayPlan = bayPlanRepositorio.findById(plano.getBayPlanId())
                .orElseThrow(() -> new EntityNotFoundException("Bay Plan não encontrado: " + plano.getBayPlanId()));
        boolean todosConcluidos = !bayPlan.getContainers().isEmpty()
                && bayPlan.getContainers().stream().allMatch(this::concluido);
        boolean critica = divergencias.stream().anyMatch(item ->
                item.getStatus() == StatusDivergenciaReconciliacao.ABERTA
                        && item.getSeveridade() == SeveridadeDivergenciaReconciliacao.CRITICA);
        if (todosConcluidos && !critica) {
            bayPlan.setStatus(StatusBayPlan.CONCLUIDO);
            bayPlanRepositorio.save(bayPlan);
        } else if (critica && bayPlan.getStatus() == StatusBayPlan.CONCLUIDO) {
            bayPlan.setStatus(StatusBayPlan.EM_OPERACAO);
            bayPlanRepositorio.save(bayPlan);
        }
    }

    private ReconciliacaoBaplieExecucaoDto resposta(
            EstivagemPlan plano,
            List<DivergenciaReconciliacaoSlot> divergencias,
            LocalDateTime momento) {
        List<DivergenciaReconciliacaoDto> itens = divergencias.stream()
                .sorted(Comparator.comparing(DivergenciaReconciliacaoSlot::getCodigoContainer)
                        .thenComparing(item -> item.getTipo().name()))
                .map(DivergenciaReconciliacaoDto::deEntidade)
                .collect(Collectors.toList());
        ReconciliacaoBaplieExecucaoDto dto = new ReconciliacaoBaplieExecucaoDto();
        dto.setPlanoId(plano.getId());
        dto.setBayPlanId(plano.getBayPlanId());
        dto.setReconciliadoEm(momento);
        dto.setDivergencias(itens);
        dto.setTotalDivergencias(itens.size());
        dto.setAbertas((int) divergencias.stream().filter(this::aberta).count());
        dto.setCriticasAbertas((int) divergencias.stream().filter(this::aberta)
                .filter(item -> item.getSeveridade() == SeveridadeDivergenciaReconciliacao.CRITICA).count());
        dto.setResolvidas((int) divergencias.stream().filter(item -> !aberta(item)).count());
        return dto;
    }

    private boolean aberta(DivergenciaReconciliacaoSlot item) {
        return item.getStatus() == StatusDivergenciaReconciliacao.ABERTA;
    }

    private Map<String, BayPlanContainer> containers(BayPlan bayPlan) {
        Map<String, BayPlanContainer> mapa = new LinkedHashMap<>();
        bayPlan.getContainers().stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.hasText(item.getCodigoContainer()))
                .forEach(item -> mapa.putIfAbsent(codigo(item.getCodigoContainer()), item));
        return mapa;
    }

    private Map<String, SlotNavio> slots(EstivagemPlan plano) {
        Map<String, SlotNavio> mapa = new HashMap<>();
        plano.getSlots().stream().filter(item -> StringUtils.hasText(item.getCodigoContainer()))
                .forEach(item -> mapa.putIfAbsent(codigo(item.getCodigoContainer()), item));
        return mapa;
    }

    private String snapshotBaplie(BayPlanContainer item) {
        return item == null ? null : "posicao=" + valor(posicao(item.getPosicaoBay()))
                + ";iso=" + valor(item.getIsoCode()) + ";pesoKg=" + valor(item.getPesoOperacionalKg());
    }

    private String snapshotPlano(SlotNavio item) {
        return item == null ? null : "posicao=" + valor(posicao(item))
                + ";iso=" + valor(item.getIsoCode()) + ";pesoKg="
                + valor(item.getPesoVgmKg() != null ? item.getPesoVgmKg() : item.getPesoKg());
    }

    private String snapshotInventario(UnidadeInventario item) {
        return item == null ? null : "estado=" + valor(item.getEstado())
                + ";posicaoAtual=" + valor(item.getPosicaoAtual())
                + ";posicaoPlanejada=" + valor(item.getPosicaoPlanejada())
                + ";pesoKg=" + valor(item.getPesoBrutoKg());
    }

    private String snapshotExecucao(BayPlanContainer item) {
        return item == null ? null : "status=" + valor(item.getStatusOperacao())
                + ";posicao=" + valor(posicao(item.getPosicaoExecucao()))
                + ";pesoKg=" + valor(item.getPesoExecucaoKg())
                + ";horario=" + valor(item.getHorarioOperacao());
    }

    private String primeiraPosicao(String primeira, String segunda) {
        String valor = normalizarPosicao(primeira);
        return StringUtils.hasText(valor) ? valor : normalizarPosicao(segunda);
    }

    private String normalizarPosicao(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 6 && digitos.length() != 7) {
            return null;
        }
        PosicaoBay posicao = PosicaoBay.deCodigoEdifact(digitos);
        return posicao.getBay() == null || posicao.getRow() == null || posicao.getTier() == null
                ? null : posicao.toCodigoEdifact();
    }

    private String posicao(PosicaoBay item) {
        return item == null || item.getBay() == null || item.getRow() == null || item.getTier() == null
                ? null : item.toCodigoEdifact();
    }

    private String posicao(SlotNavio item) {
        return item == null ? null : String.format(Locale.ROOT, "%02d%02d%02d",
                item.getBay(), item.getRowBay(), item.getTier());
    }

    private boolean diferente(String primeiro, String segundo) {
        return StringUtils.hasText(primeiro) && StringUtils.hasText(segundo) && !primeiro.equals(segundo);
    }

    private boolean concluido(BayPlanContainer item) {
        return "CONCLUIDO".equalsIgnoreCase(item.getStatusOperacao());
    }

    private void peso(List<Double> pesos, BigDecimal valor) {
        peso(pesos, valor == null ? null : valor.doubleValue());
    }

    private void peso(List<Double> pesos, Double valor) {
        if (valor != null && Double.isFinite(valor) && valor > 0) {
            pesos.add(valor);
        }
    }

    private String assinatura(String... valores) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String valor : valores) {
                digest.update((valor == null ? "<null>" : valor).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException("SHA-256 indisponível.", excecao);
        }
    }

    private EstivagemPlan plano(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do plano é obrigatório.");
        }
        return planRepositorio.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + id));
    }

    private String usuario() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao == null || !autenticacao.isAuthenticated()
                || !StringUtils.hasText(autenticacao.getName())
                || "anonymousUser".equalsIgnoreCase(autenticacao.getName())
                ? "SISTEMA" : autenticacao.getName();
    }

    private String codigo(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String valor(Object valor) {
        return valor == null ? "NAO_INFORMADO" : String.valueOf(valor);
    }

    private record Detectada(
            String chave,
            String codigo,
            TipoDivergenciaReconciliacao tipo,
            SeveridadeDivergenciaReconciliacao severidade,
            SlotNavio slot,
            String baplie,
            String plano,
            String inventario,
            String execucao,
            String assinatura) {
    }
}
