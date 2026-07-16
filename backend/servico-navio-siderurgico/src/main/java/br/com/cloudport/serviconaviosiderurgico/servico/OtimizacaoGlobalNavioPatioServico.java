package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OtimizacaoYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.OtimizacaoGlobalNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ReservaPatioNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OtimizacaoGlobalNavioPatioServico {

    private final VisitaNavioServico visitaServico;
    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final ReservaPatioNavioServico reservaPatioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;
    private final OtimizacaoYardCliente otimizacaoYardCliente;
    private final EventoOperacionalStreamingServico streamingServico;

    public OtimizacaoGlobalNavioPatioServico(
            VisitaNavioServico visitaServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            ReservaPatioNavioServico reservaPatioServico,
            OrdemPatioYardCliente ordemPatioYardCliente,
            OtimizacaoYardCliente otimizacaoYardCliente,
            EventoOperacionalStreamingServico streamingServico
    ) {
        this.visitaServico = visitaServico;
        this.itemRepositorio = itemRepositorio;
        this.reservaPatioServico = reservaPatioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
        this.otimizacaoYardCliente = otimizacaoYardCliente;
        this.streamingServico = streamingServico;
    }

    @Transactional(readOnly = true)
    public OtimizacaoGlobalNavioPatioDTO otimizar(Long visitaId) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioId(visitaId);
        Map<Long, ReservaPatioNavioDTO> reservaPorItem = reservasComPosicao(visitaId);
        List<String> equipamentos = equipamentosReais(visitaId);
        if (equipamentos.isEmpty()) {
            throw new IllegalArgumentException("A otimizacao global exige ao menos um equipamento alocado em work queue real.");
        }

        List<Map<String, Object>> importacao = new ArrayList<>();
        List<Map<String, Object>> exportacao = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        int semPosicao = 0;
        for (ItemOperacaoNavio item : itens) {
            ReservaPatioNavioDTO reserva = reservaPorItem.get(item.getId());
            if (reserva == null || reserva.linha() == null || reserva.coluna() == null) {
                semPosicao++;
                continue;
            }
            Map<String, Object> container = Map.of(
                    "codigoContainer", item.getCodigoLote(),
                    "linha", reserva.linha(),
                    "coluna", reserva.coluna()
            );
            if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA) {
                importacao.add(container);
            } else if (item.getTipoMovimento() == TipoMovimentoNavio.EMBARQUE) {
                exportacao.add(container);
            } else {
                alertas.add("O item " + item.getCodigoLote() + " e RESTOW e permanece fora do dual-cycle do Yard.");
            }
        }
        if (importacao.isEmpty() && exportacao.isEmpty()) {
            throw new IllegalArgumentException("Nao existem itens de embarque ou descarga com reserva real para otimizar.");
        }

        LocalDateTime chegada = primeiraData(visita.getAtb(), visita.getEtb(), visita.getEta());
        LocalDateTime partida = primeiraData(visita.getEtd(), visita.getFimOperacao(), visita.getAtd());
        if (chegada == null || partida == null || !partida.isAfter(chegada)) {
            throw new IllegalArgumentException("A otimizacao global exige janela real ou estimada valida entre chegada/atracacao e partida.");
        }
        String berco = StringUtils.hasText(visita.getBercoAtual()) ? visita.getBercoAtual() : visita.getBercoPrevisto();
        if (!StringUtils.hasText(berco)) {
            throw new IllegalArgumentException("A otimizacao global exige berco atual ou previsto.");
        }

        Map<String, Object> navio = new LinkedHashMap<>();
        navio.put("codigoNavio", visita.getCodigoVisita());
        navio.put("nomeBerco", berco);
        navio.put("etaChegada", chegada);
        navio.put("etaPartida", partida);
        navio.put("quantidadeContainersImportacao", importacao.size());
        navio.put("quantidadeContainersExportacao", exportacao.size());
        navio.put("prioridade", visita.getFase().name());
        navio.put("observacoes", "Plano integrado gerado pelo modulo Navio Siderurgico.");

        Map<String, Object> requisicao = new LinkedHashMap<>();
        requisicao.put("navio", navio);
        requisicao.put("equipamentosDisponiveis", equipamentos);
        requisicao.put("containersImportacao", importacao);
        requisicao.put("containersExportacao", exportacao);

        Map<String, Object> plano = otimizacaoYardCliente.otimizar(requisicao);
        String status = semPosicao == 0 ? "OTIMIZADO" : "OTIMIZADO_PARCIALMENTE";
        if (semPosicao > 0) {
            alertas.add(semPosicao + " item(ns) ficaram fora da otimizacao por ausencia de posicao real no patio.");
        }
        OtimizacaoGlobalNavioPatioDTO resultado = new OtimizacaoGlobalNavioPatioDTO(
                visitaId,
                LocalDateTime.now(),
                equipamentos.size(),
                importacao.size(),
                exportacao.size(),
                semPosicao,
                status,
                List.copyOf(alertas),
                plano
        );
        streamingServico.publicar(
                visitaId,
                "OTIMIZACAO_GLOBAL",
                "PLANO_GLOBAL_OTIMIZADO",
                Map.of(
                        "status", status,
                        "equipamentos", equipamentos.size(),
                        "importacao", importacao.size(),
                        "exportacao", exportacao.size(),
                        "itensSemPosicao", semPosicao
                )
        );
        return resultado;
    }

    private Map<Long, ReservaPatioNavioDTO> reservasComPosicao(Long visitaId) {
        Map<Long, ReservaPatioNavioDTO> porItem = new LinkedHashMap<>();
        reservaPatioServico.listar(visitaId).stream()
                .filter(reserva -> reserva.status() == StatusReservaPatioNavio.ATIVA
                        || reserva.status() == StatusReservaPatioNavio.CONSUMIDA)
                .filter(reserva -> reserva.itemOperacaoNavioId() != null)
                .forEach(reserva -> porItem.put(reserva.itemOperacaoNavioId(), reserva));
        return porItem;
    }

    private List<String> equipamentosReais(Long visitaId) {
        return ordemPatioYardCliente.listarWorkQueuesDaVisita(visitaId).stream()
                .map(WorkQueuePatioYardDTO::getEquipamento)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private LocalDateTime primeiraData(LocalDateTime... datas) {
        for (LocalDateTime data : datas) {
            if (Objects.nonNull(data)) {
                return data;
            }
        }
        return null;
    }
}
