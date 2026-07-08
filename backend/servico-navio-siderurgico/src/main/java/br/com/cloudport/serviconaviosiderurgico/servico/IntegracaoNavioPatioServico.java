package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
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
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.RelatorioOperacionalIntegradoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
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
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IntegracaoNavioPatioServico {

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private final VisitaNavioServico visitaServico;
    private final PlanoEstivaNavioServico planoServico;
    private final ReservaPatioNavioServico reservaPatioServico;
    private final ValidadorIntegracaoNavioPatioServico validador;
    private final SincronizadorStatusNavioPatioServico sincronizador;
    private final OrdemPatioYardCliente ordemPatioYardCliente;

    public IntegracaoNavioPatioServico(
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPosicaoPatioNavioRepositorio reservaRepositorio,
            VisitaNavioServico visitaServico,
            PlanoEstivaNavioServico planoServico,
            ReservaPatioNavioServico reservaPatioServico,
            ValidadorIntegracaoNavioPatioServico validador,
            SincronizadorStatusNavioPatioServico sincronizador,
            OrdemPatioYardCliente ordemPatioYardCliente
    ) {
        this.itemRepositorio = itemRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.visitaServico = visitaServico;
        this.planoServico = planoServico;
        this.reservaPatioServico = reservaPatioServico;
        this.validador = validador;
        this.sincronizador = sincronizador;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
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
                .filter(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.EM_EXECUCAO || item.getStatus() == StatusItemCarga.EM_MOVIMENTO)
                .count();
        long ordensConcluidas = itens.stream()
                .filter(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.SINCRONIZADO || item.getStatus() == StatusItemCarga.OPERADO)
                .count();
        return new ResumoIntegracaoNavioPatioDTO(
                visitaId,
                itens.size(),
                itensComReserva,
                itensComOrdem,
                Math.max(0, itens.stream().filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA).count() - itensComReserva),
                Math.max(0, itens.size() - itensComOrdem),
                ordensEmExecucao,
                ordensConcluidas,
                alertas.size(),
                statusPredominante(itens)
        );
    }

    @Transactional
    public List<ReservaPatioNavioDTO> gerarReservasDaVisita(Long visitaId, ComandoGeracaoReservasPatioDTO comando) {
        return reservaPatioServico.gerarReservasDaVisita(visitaId, comando);
    }

    @Transactional(readOnly = true)
    public List<ReservaPatioNavioDTO> listarReservasDaVisita(Long visitaId) {
        return reservaPatioServico.listar(visitaId);
    }

    @Transactional
    public ResultadoGeracaoOrdensPatioDTO gerarOrdensDaVisita(Long visitaId, ComandoGeracaoOrdensPatioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        ComandoGeracaoOrdensPatioDTO comandoEfetivo = comando == null ? new ComandoGeracaoOrdensPatioDTO(null, null, null, null) : comando;
        if (comandoEfetivo.gerarReservasAutomaticasEfetivo()) {
            reservaPatioServico.gerarReservasDaVisita(visitaId, new ComandoGeracaoReservasPatioDTO(TipoReservaPatioNavio.TENTATIVA, true, comandoEfetivo.usuario()));
        }
        List<String> erros = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        int criadas = 0;
        int ignoradas = 0;
        int comErro = 0;

        List<ItemOperacaoNavio> itens = listarItens(visitaId).stream()
                .filter(item -> comandoEfetivo.tipoMovimento() == null || item.getTipoMovimento() == comandoEfetivo.tipoMovimento())
                .filter(item -> item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(ItemOperacaoNavio::getSequenciaOperacional, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (ItemOperacaoNavio item : itens) {
            if (comandoEfetivo.modoEfetivo() == ModoGeracaoOrdensPatio.SOMENTE_PENDENTES && item.getOrdemTrabalhoPatioId() != null) {
                ignoradas++;
                continue;
            }
            List<String> errosItem = validador.validarGeracaoOrdem(item);
            if (!errosItem.isEmpty()) {
                comErro++;
                errosItem.forEach(erro -> erros.add("Item " + item.getCodigoLote() + ": " + erro));
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ERRO);
                itemRepositorio.save(item);
                continue;
            }
            ReservaPosicaoPatioNavio reservaAtiva = obterReservaAtiva(item);
            if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA && reservaAtiva != null) {
                item.setPosicaoPatioPlanejada(reservaAtiva.getPosicaoPatioId());
            }
            try {
                boolean jaPossuiaOrdem = item.getOrdemTrabalhoPatioId() != null;
                var ordemYard = ordemPatioYardCliente.criarOuReutilizarOrdem(item, reservaAtiva);
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
                visitaServico.registrarEvento(visita, item, "ORDEM_PATIO_REAL_GERADA", "Ordem real de patio vinculada ao item " + item.getCodigoLote() + ".", comandoEfetivo.usuario(), null, String.valueOf(item.getOrdemTrabalhoPatioId()));
            } catch (RuntimeException ex) {
                comErro++;
                erros.add("Item " + item.getCodigoLote() + ": falha ao criar ordem real no servico-yard - " + ex.getMessage());
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ERRO);
                itemRepositorio.save(item);
                visitaServico.registrarEvento(visita, item, "ORDEM_PATIO_REAL_ERRO", "Falha ao criar ordem real no servico-yard para o item " + item.getCodigoLote() + ".", comandoEfetivo.usuario(), null, ex.getMessage());
            }
        }

        List<AlertaIntegracaoNavioPatioDTO> alertasIntegracao = validador.listarAlertas(visitaId, listarItens(visitaId));
        alertasIntegracao.forEach(alerta -> alertas.add(alerta.tipo() + ": " + alerta.mensagem()));
        visitaServico.registrarEvento(visita, null, "ORDENS_PATIO_REAIS_GERADAS", criadas + " ordem(ns) real(is) de patio vinculada(s) pela visita.", comandoEfetivo.usuario(), null, String.valueOf(criadas));
        return new ResultadoGeracaoOrdensPatioDTO(criadas, ignoradas, comErro, erros, alertas);
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
    public ResultadoReplanejamentoPatioNavioDTO replanejarPatioDaVisita(Long visitaId, ComandoReplanejamentoPatioNavioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        List<String> alertas = new ArrayList<>();
        List<Long> naoReplanejados = new ArrayList<>();
        boolean aplicar = comando != null && comando.aplicarEfetivo();

        List<ItemOperacaoNavio> itensDescarga = listarItens(visitaId).stream()
                .filter(item -> item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA)
                .filter(item -> item.getStatus() != StatusItemCarga.OPERADO && item.getStatus() != StatusItemCarga.CANCELADO)
                .sorted(Comparator.comparing(ItemOperacaoNavio::getSequenciaOperacional, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<ReservaPatioNavioDTO> reservas = new ArrayList<>();
        int sequencia = 1;
        for (ItemOperacaoNavio item : itensDescarga) {
            if (item.getStatus() == StatusItemCarga.BLOQUEADO) {
                naoReplanejados.add(item.getId());
                alertas.add("Item " + item.getCodigoLote() + " bloqueado nao foi replanejado.");
                continue;
            }
            String posicaoSugerida = "RP-" + visitaId + "-" + sequencia;
            ReservaPosicaoPatioNavio reserva = reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(item.getId(), List.of(StatusReservaPatioNavio.ATIVA))
                    .orElseGet(() -> novaReserva(item));
            reserva.setPosicaoPatioId(posicaoSugerida);
            reserva.setBloco("RP");
            reserva.setLinha(sequencia);
            reserva.setColuna(1);
            reserva.setCamada("A");
            reserva.setTipoReserva(aplicar ? TipoReservaPatioNavio.DEFINITIVA : TipoReservaPatioNavio.TENTATIVA);
            if (aplicar) {
                ReservaPosicaoPatioNavio salva = reservaRepositorio.save(reserva);
                item.setPosicaoPatioPlanejada(posicaoSugerida);
                item.setDestinoPatio(posicaoSugerida);
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
                itemRepositorio.save(item);
                reservas.add(ReservaPatioNavioDTO.de(salva));
            } else {
                reservas.add(ReservaPatioNavioDTO.de(reserva));
            }
            sequencia++;
        }

        if (aplicar) {
            visitaServico.registrarEvento(visita, null, "REPLANEJAMENTO_PATIO_APLICADO", reservas.size() + " reserva(s) replanejada(s) para a visita.", comando.usuario(), null, String.valueOf(reservas.size()));
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
        List<ItemOperacaoNavioDTO> itens = listarItens(visitaId).stream().map(ItemOperacaoNavioDTO::de).toList();
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
        return reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(item.getId(), List.of(StatusReservaPatioNavio.ATIVA))
                .orElse(null);
    }

    private StatusIntegracaoPatio statusPredominante(List<ItemOperacaoNavio> itens) {
        if (itens.stream().anyMatch(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.ERRO)) {
            return StatusIntegracaoPatio.ERRO;
        }
        if (itens.stream().anyMatch(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.EM_EXECUCAO)) {
            return StatusIntegracaoPatio.EM_EXECUCAO;
        }
        if (!itens.isEmpty() && itens.stream().allMatch(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.SINCRONIZADO || item.getStatus() == StatusItemCarga.OPERADO)) {
            return StatusIntegracaoPatio.SINCRONIZADO;
        }
        if (itens.stream().anyMatch(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.ORDEM_GERADA)) {
            return StatusIntegracaoPatio.ORDEM_GERADA;
        }
        if (itens.stream().anyMatch(item -> item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.RESERVADO)) {
            return StatusIntegracaoPatio.RESERVADO;
        }
        return StatusIntegracaoPatio.NAO_GERADO;
    }

    private ReservaPosicaoPatioNavio novaReserva(ItemOperacaoNavio item) {
        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        return reserva;
    }

    private PlanoEstivaNavioDTO obterPlanoSeExistir(Long visitaId) {
        try {
            return planoServico.obter(visitaId);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
