package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.FilaOrdemPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.comum.IntegracaoYardIndisponivelException;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ModoGeracaoOrdensPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AlertaIntegracaoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoGeracaoOrdensPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoGeracaoReservasPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.EventoVisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.FilaPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.RelatorioOperacionalIntegradoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoConsultaYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoGeracaoOrdensPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResultadoReplanejamentoPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoIntegracaoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ResumoOperacionalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IntegracaoNavioPatioServico {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegracaoNavioPatioServico.class);

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final VisitaNavioServico visitaServico;
    private final PlanoEstivaNavioServico planoServico;
    private final ReservaPatioNavioServico reservaPatioServico;
    private final ValidadorIntegracaoNavioPatioServico validador;
    private final SincronizadorStatusNavioPatioServico sincronizador;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final boolean contingenciaConsultasYardHabilitada;

    public IntegracaoNavioPatioServico(
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            VisitaNavioServico visitaServico,
            PlanoEstivaNavioServico planoServico,
            ReservaPatioNavioServico reservaPatioServico,
            ValidadorIntegracaoNavioPatioServico validador,
            SincronizadorStatusNavioPatioServico sincronizador,
            OrdemPatioYardCliente ordemPatioYardCliente,
            @Value("${cloudport.integracao.yard.contingencia-consultas-enabled:false}")
            boolean contingenciaConsultasYardHabilitada
    ) {
        this.itemRepositorio = itemRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.visitaServico = visitaServico;
        this.planoServico = planoServico;
        this.reservaPatioServico = reservaPatioServico;
        this.validador = validador;
        this.sincronizador = sincronizador;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.contingenciaConsultasYardHabilitada = contingenciaConsultasYardHabilitada;
    }

    @Transactional(readOnly = true)
    public ResumoIntegracaoNavioPatioDTO obterResumoIntegracao(Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = listarItens(visitaId);
        List<AlertaIntegracaoNavioPatioDTO> alertas = validador.listarAlertas(visitaId, itens);
        long itensComReserva = itens.stream()
                .filter(item -> StringUtils.hasText(item.getPosicaoPatioPlanejada()))
                .count();
        long itensComOrdem = itens.stream()
                .filter(item -> item.getOrdemTrabalhoPatioId() != null)
                .count();
        long ordensEmExecucao = itens.stream()
                .filter(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.EM_EXECUCAO
                        || item.getStatus() == StatusItemCarga.EM_MOVIMENTO)
                .count();
        long ordensConcluidas = itens.stream()
                .filter(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.SINCRONIZADO
                        || item.getStatus() == StatusItemCarga.OPERADO)
                .count();
        return new ResumoIntegracaoNavioPatioDTO(
                visitaId,
                itens.size(),
                itensComReserva,
                itensComOrdem,
                Math.max(0, itens.stream()
                        .filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA)
                        .count() - itensComReserva),
                Math.max(0, itens.size() - itensComOrdem),
                ordensEmExecucao,
                ordensConcluidas,
                alertas.size(),
                statusPredominante(itens)
        );
    }

    @Transactional
    public List<ReservaPatioNavioDTO> gerarReservasDaVisita(
            Long visitaId,
            ComandoGeracaoReservasPatioDTO comando) {
        return reservaPatioServico.gerarReservasDaVisita(visitaId, comando);
    }

    @Transactional(readOnly = true)
    public List<ReservaPatioNavioDTO> listarReservasDaVisita(Long visitaId) {
        return reservaPatioServico.listar(visitaId);
    }

    @Transactional
    public ResultadoGeracaoOrdensPatioDTO gerarOrdensDaVisita(
            Long visitaId,
            ComandoGeracaoOrdensPatioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        ComandoGeracaoOrdensPatioDTO comandoEfetivo = comando == null
                ? new ComandoGeracaoOrdensPatioDTO(null, null, null, null)
                : comando;
        if (comandoEfetivo.gerarReservasAutomaticasEfetivo()) {
            reservaPatioServico.gerarReservasDaVisita(
                    visitaId,
                    new ComandoGeracaoReservasPatioDTO(
                            TipoReservaPatioNavio.TENTATIVA,
                            true,
                            comandoEfetivo.usuario()));
        }
        List<String> erros = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        int criadas = 0;
        int ignoradas = 0;
        int comErro = 0;

        List<ItemOperacaoNavio> itens = listarItens(visitaId).stream()
                .filter(item -> comandoEfetivo.tipoMovimento() == null
                        || item.getTipoMovimento() == comandoEfetivo.tipoMovimento())
                .filter(item -> item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(
                        ItemOperacaoNavio::getSequenciaOperacional,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (ItemOperacaoNavio item : itens) {
            if (comandoEfetivo.modoEfetivo() == ModoGeracaoOrdensPatio.SOMENTE_PENDENTES
                    && item.getOrdemTrabalhoPatioId() != null) {
                ignoradas++;
                continue;
            }
            List<String> errosItem = validador.validarGeracaoOrdem(item);
            if (!errosItem.isEmpty()) {
                comErro++;
                errosItem.forEach(erro -> erros.add(
                        "Item " + item.getCodigoLote() + ": " + erro));
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ERRO);
                itemRepositorio.save(item);
                continue;
            }
            ReservaPosicaoPatioNavio reservaAtiva = obterReservaAtiva(item);
            if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA
                    && reservaAtiva != null) {
                item.setPosicaoPatioPlanejada(reservaAtiva.getPosicaoPatioId());
            }
            try {
                boolean jaPossuiaOrdem = item.getOrdemTrabalhoPatioId() != null;
                var ordemYard = ordemPatioYardCliente.criarOuReutilizarOrdem(
                        item,
                        reservaAtiva);
                item.setOrdemTrabalhoPatioId(ordemYard.getId());
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ORDEM_GERADA);
                if (item.getStatus() == StatusItemCarga.PLANEJADO) {
                    item.setStatus(StatusItemCarga.LIBERADO);
                }
                itemRepositorio.save(item);
                if (jaPossuiaOrdem) {
                    ignoradas++;
                } else {
                    criadas++;
                }
                visitaServico.registrarEvento(
                        visita,
                        item,
                        "ORDEM_PATIO_REAL_GERADA",
                        "Ordem real de patio vinculada ao item "
                                + item.getCodigoLote() + ".",
                        comandoEfetivo.usuario(),
                        null,
                        String.valueOf(item.getOrdemTrabalhoPatioId()));
            } catch (RuntimeException ex) {
                comErro++;
                erros.add("Item " + item.getCodigoLote()
                        + ": falha ao criar ordem real no servico-yard - "
                        + ex.getMessage());
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ERRO);
                itemRepositorio.save(item);
                visitaServico.registrarEvento(
                        visita,
                        item,
                        "ORDEM_PATIO_REAL_ERRO",
                        "Falha ao criar ordem real no servico-yard para o item "
                                + item.getCodigoLote() + ".",
                        comandoEfetivo.usuario(),
                        null,
                        ex.getMessage());
            }
        }

        List<AlertaIntegracaoNavioPatioDTO> alertasIntegracao = validador
                .listarAlertas(visitaId, listarItens(visitaId));
        alertasIntegracao.forEach(alerta -> alertas.add(
                alerta.tipo() + ": " + alerta.mensagem()));
        visitaServico.registrarEvento(
                visita,
                null,
                "ORDENS_PATIO_REAIS_GERADAS",
                criadas + " ordem(ns) real(is) de patio vinculada(s) pela visita.",
                comandoEfetivo.usuario(),
                null,
                String.valueOf(criadas));
        return new ResultadoGeracaoOrdensPatioDTO(
                criadas,
                ignoradas,
                comErro,
                erros,
                alertas);
    }

    @Transactional(readOnly = true)
    public List<OrdemPatioDaVisitaDTO> listarOrdensDaVisita(Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        return listarItens(visitaId).stream()
                .filter(item -> item.getOrdemTrabalhoPatioId() != null)
                .map(OrdemPatioDaVisitaDTO::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultadoConsultaYardDTO<FilaPatioDaVisitaDTO> listarFilasOperacionaisDaVisita(
            Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        try {
            List<FilaPatioDaVisitaDTO> filas = ordemPatioYardCliente.listarFilasDaVisita(visitaId).stream()
                    .map(this::converterFilaYard)
                    .toList();
            return ResultadoConsultaYardDTO.confirmada(filas);
        } catch (RuntimeException ex) {
            IntegracaoYardIndisponivelException falha = tiparFalhaConsultaYard(
                    "a consulta de filas operacionais",
                    ex);
            if (!contingenciaConsultasYardHabilitada) {
                throw falha;
            }
            LOGGER.warn(
                    "Consulta de filas do Yard executada em contingencia. visitaId={} motivo={}",
                    visitaId,
                    falha.getReason());
            return ResultadoConsultaYardDTO.degradada(
                    agruparFilasLocais(visitaId),
                    falha.getReason());
        }
    }

    @Transactional(readOnly = true)
    public ResultadoConsultaYardDTO<OrdemPatioDaVisitaDTO> listarOrdensSemCoberturaDaVisita(
            Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        try {
            List<OrdemPatioDaVisitaDTO> ordens = ordemPatioYardCliente
                    .listarOrdensSemCobertura(visitaId).stream()
                    .map(this::converterOrdemYard)
                    .toList();
            return ResultadoConsultaYardDTO.confirmada(ordens);
        } catch (RuntimeException ex) {
            IntegracaoYardIndisponivelException falha = tiparFalhaConsultaYard(
                    "a consulta de ordens sem cobertura",
                    ex);
            if (!contingenciaConsultasYardHabilitada) {
                throw falha;
            }
            LOGGER.warn(
                    "Consulta de cobertura do Yard executada em contingencia. visitaId={} motivo={}",
                    visitaId,
                    falha.getReason());
            List<OrdemPatioDaVisitaDTO> ordensDerivadas = listarOrdensDaVisita(visitaId).stream()
                    .filter(ordem -> ordem.prioridadeOperacional() == null
                            || ordem.sequenciaNavio() == null
                            || !StringUtils.hasText(ordem.destino()))
                    .toList();
            return ResultadoConsultaYardDTO.degradada(
                    ordensDerivadas,
                    falha.getReason());
        }
    }

    @Transactional(readOnly = true)
    public List<AlertaIntegracaoNavioPatioDTO> listarAlertasIntegracao(Long visitaId) {
        visitaServico.buscarEntidade(visitaId);
        return validador.listarAlertas(visitaId, listarItens(visitaId));
    }

    @Transactional
    public ResumoIntegracaoNavioPatioDTO sincronizarStatus(Long visitaId) {
        sincronizador.sincronizarStatus(visitaId);
        return obterResumoIntegracao(visitaId);
    }

    @Transactional
    public ResultadoReplanejamentoPatioNavioDTO replanejarPatioDaVisita(
            Long visitaId,
            ComandoReplanejamentoPatioNavioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        List<String> alertas = new ArrayList<>();
        List<Long> naoReplanejados = new ArrayList<>();
        boolean aplicar = comando != null && comando.aplicarEfetivo();
        String usuario = comando == null || !StringUtils.hasText(comando.usuario())
                ? "sistema"
                : comando.usuario();

        List<ItemOperacaoNavio> itensDescarga = listarItens(visitaId).stream()
                .filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA)
                .filter(item -> item.getStatus() != StatusItemCarga.OPERADO
                        && item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(
                        ItemOperacaoNavio::getSequenciaOperacional,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<ReservaPatioNavioDTO> reservas = new ArrayList<>();
        for (ItemOperacaoNavio item : itensDescarga) {
            if (item.getStatus() == StatusItemCarga.BLOQUEADO) {
                naoReplanejados.add(item.getId());
                alertas.add("Item " + item.getCodigoLote()
                        + " bloqueado nao foi replanejado.");
                continue;
            }
            try {
                ReservaPosicaoPatioNavio reserva = aplicar
                        ? reservaPatioServico.replanejarItem(
                                item,
                                TipoReservaPatioNavio.DEFINITIVA,
                                usuario)
                        : reservaPatioServico.simularReplanejamentoItem(
                                item,
                                TipoReservaPatioNavio.TENTATIVA);
                reservas.add(ReservaPatioNavioDTO.de(reserva));
            } catch (RuntimeException ex) {
                naoReplanejados.add(item.getId());
                alertas.add("Item " + item.getCodigoLote()
                        + " nao foi replanejado: " + ex.getMessage());
            }
        }

        if (aplicar) {
            visitaServico.registrarEvento(
                    visita,
                    null,
                    "REPLANEJAMENTO_PATIO_APLICADO",
                    reservas.size()
                            + " reserva(s) replanejada(s) com compensacao da reserva anterior.",
                    usuario,
                    null,
                    String.valueOf(reservas.size()));
        }
        return new ResultadoReplanejamentoPatioNavioDTO(
                reservas,
                listarOrdensDaVisita(visitaId),
                BigDecimal.valueOf(reservas.isEmpty() ? 0 : 12),
                naoReplanejados.isEmpty() ? "BAIXO" : "MEDIO",
                alertas,
                naoReplanejados
        );
    }

    @Transactional(readOnly = true)
    public RelatorioOperacionalIntegradoDTO gerarRelatorioOperacionalIntegrado(Long visitaId) {
        var visita = visitaServico.buscarEntidade(visitaId);
        ResumoOperacionalNavioDTO resumoOperacional = visitaServico.resumo(visitaId);
        ResumoIntegracaoNavioPatioDTO resumoIntegracao = obterResumoIntegracao(visitaId);
        PlanoEstivaNavioDTO plano = obterPlanoSeExistir(visitaId);
        List<ItemOperacaoNavioDTO> itens = listarItens(visitaId).stream()
                .map(ItemOperacaoNavioDTO::de)
                .toList();
        List<EventoVisitaNavioDTO> eventos = visitaServico.eventos(visitaId);
        return new RelatorioOperacionalIntegradoDTO(
                VisitaNavioDTO.de(visita),
                resumoOperacional,
                resumoIntegracao,
                plano,
                itens,
                listarReservasDaVisita(visitaId),
                listarOrdensDaVisita(visitaId),
                listarAlertasIntegracao(visitaId),
                eventos
        );
    }

    private List<ItemOperacaoNavio> listarItens(Long visitaId) {
        return itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId);
    }

    private ReservaPosicaoPatioNavio obterReservaAtiva(ItemOperacaoNavio item) {
        return reservaRepositorio
                .findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                        item.getId(),
                        List.of(StatusReservaPatioNavio.ATIVA))
                .orElse(null);
    }

    private StatusIntegracaoPatio statusPredominante(List<ItemOperacaoNavio> itens) {
        if (itens.stream().anyMatch(
                item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.ERRO)) {
            return StatusIntegracaoPatio.ERRO;
        }
        if (itens.stream().anyMatch(
                item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.EM_EXECUCAO)) {
            return StatusIntegracaoPatio.EM_EXECUCAO;
        }
        if (!itens.isEmpty() && itens.stream().allMatch(
                item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.SINCRONIZADO
                        || item.getStatus() == StatusItemCarga.OPERADO)) {
            return StatusIntegracaoPatio.SINCRONIZADO;
        }
        if (itens.stream().anyMatch(
                item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.ORDEM_GERADA)) {
            return StatusIntegracaoPatio.ORDEM_GERADA;
        }
        if (itens.stream().anyMatch(
                item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.RESERVADO)) {
            return StatusIntegracaoPatio.RESERVADO;
        }
        return StatusIntegracaoPatio.NAO_GERADO;
    }

    private PlanoEstivaNavioDTO obterPlanoSeExistir(Long visitaId) {
        try {
            return planoServico.obter(visitaId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private FilaPatioDaVisitaDTO converterFilaYard(FilaOrdemPatioYardDTO fila) {
        List<OrdemPatioDaVisitaDTO> ordens = Optional.ofNullable(fila.getOrdens())
                .orElse(List.of()).stream()
                .map(this::converterOrdemYard)
                .toList();
        return new FilaPatioDaVisitaDTO(
                fila.getIdentificador(),
                fila.getAgrupamento(),
                fila.getVisitaNavioId(),
                fila.getBerco(),
                fila.getBlocoZona(),
                fila.getSequenciaInicial(),
                fila.getStatus(),
                fila.getTotalOrdens(),
                ordens
        );
    }

    private OrdemPatioDaVisitaDTO converterOrdemYard(OrdemPatioYardRespostaDTO ordem) {
        TipoMovimentoNavio tipoMovimento = tipoMovimentoNavio(ordem.getTipoMovimento());
        String destinoFormatado = ordem.posicaoDestinoFormatada();
        return new OrdemPatioDaVisitaDTO(
                ordem.getId(),
                ordem.getVisitaNavioId(),
                ordem.getItemOperacaoNavioId(),
                ordem.getCodigoConteiner(),
                tipoMovimento,
                ordem.getStatusOrdem(),
                tipoMovimento == TipoMovimentoNavio.DESCARGA
                        ? "NAVIO"
                        : ordem.getDestino(),
                tipoMovimento == TipoMovimentoNavio.EMBARQUE
                        ? "NAVIO"
                        : ordem.getDestino(),
                destinoFormatado,
                null,
                ordem.getSequenciaNavio(),
                ordem.getPrioridadeOperacional()
        );
    }

    private TipoMovimentoNavio tipoMovimentoNavio(String tipoMovimentoPatio) {
        if (!StringUtils.hasText(tipoMovimentoPatio)) {
            return TipoMovimentoNavio.DESCARGA;
        }
        return switch (tipoMovimentoPatio.toUpperCase(Locale.ROOT)) {
            case "TRANSFERENCIA" -> TipoMovimentoNavio.EMBARQUE;
            case "REMANEJAMENTO" -> TipoMovimentoNavio.RESTOW;
            default -> TipoMovimentoNavio.DESCARGA;
        };
    }

    private IntegracaoYardIndisponivelException tiparFalhaConsultaYard(
            String operacao,
            RuntimeException ex) {
        if (ex instanceof IntegracaoYardIndisponivelException falhaTipada) {
            return falhaTipada;
        }
        return new IntegracaoYardIndisponivelException(operacao, ex);
    }

    private List<FilaPatioDaVisitaDTO> agruparFilasLocais(Long visitaId) {
        List<OrdemPatioDaVisitaDTO> ordens = listarOrdensDaVisita(visitaId);
        Map<String, List<OrdemPatioDaVisitaDTO>> agrupadas = ordens.stream()
                .collect(Collectors.groupingBy(
                        this::chaveFilaLocal,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return agrupadas.entrySet().stream()
                .map(entry -> montarFilaLocal(
                        visitaId,
                        entry.getKey(),
                        entry.getValue()))
                .toList();
    }

    private FilaPatioDaVisitaDTO montarFilaLocal(
            Long visitaId,
            String chave,
            List<OrdemPatioDaVisitaDTO> ordens) {
        String[] partes = chave.split("\\|");
        String berco = partes.length > 0 ? partes[0] : null;
        String blocoZona = partes.length > 1 ? partes[1] : null;
        String status = partes.length > 2 ? partes[2] : null;
        return new FilaPatioDaVisitaDTO(
                chave,
                "VISITA_BERCO_ZONA_STATUS_LOCAL",
                visitaId,
                berco,
                blocoZona,
                ordens.stream()
                        .map(OrdemPatioDaVisitaDTO::sequenciaNavio)
                        .filter(java.util.Objects::nonNull)
                        .min(Integer::compareTo)
                        .orElse(null),
                status,
                ordens.size(),
                ordens
        );
    }

    private String chaveFilaLocal(OrdemPatioDaVisitaDTO ordem) {
        String berco = valor(ordem.destino(), "SEM_BERCO");
        String blocoZona = valor(ordem.posicaoPlanejada(), "SEM_ZONA");
        String status = valor(ordem.statusOrdem(), "SEM_STATUS");
        return berco + "|" + blocoZona + "|" + status;
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor)
                ? valor.trim().toUpperCase(Locale.ROOT)
                : padrao;
    }
}
