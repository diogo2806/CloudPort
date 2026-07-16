package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SincronizadorStatusNavioPatioServico {

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;
    private final ReservaPatioNavioServico reservaPatioServico;
    private final OrdemPatioYardCliente ordemPatioYardCliente;

    public SincronizadorStatusNavioPatioServico(
            ItemOperacaoNavioRepositorio itemRepositorio,
            VisitaNavioServico visitaServico,
            ReservaPatioNavioServico reservaPatioServico,
            OrdemPatioYardCliente ordemPatioYardCliente
    ) {
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
        this.reservaPatioServico = reservaPatioServico;
        this.ordemPatioYardCliente = ordemPatioYardCliente;
    }

    @Transactional
    public int sincronizarStatus(Long visitaId) {
        var visita = visitaServico.buscarEntidade(visitaId);
        int alterados = 0;
        List<ItemOperacaoNavio> itens = itemRepositorio
                .findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId);
        List<OrdemPatioYardRespostaDTO> ordensYard = ordemPatioYardCliente
                .listarOrdensDaVisita(visitaId);
        Map<Long, OrdemPatioYardRespostaDTO> ordensPorItem = ordensYard.stream()
                .filter(ordem -> ordem.getItemOperacaoNavioId() != null)
                .collect(Collectors.toMap(
                        OrdemPatioYardRespostaDTO::getItemOperacaoNavioId,
                        Function.identity(),
                        (primeira, segunda) -> primeira));
        Map<Long, OrdemPatioYardRespostaDTO> ordensPorId = ordensYard.stream()
                .filter(ordem -> ordem.getId() != null)
                .collect(Collectors.toMap(
                        OrdemPatioYardRespostaDTO::getId,
                        Function.identity(),
                        (primeira, segunda) -> primeira));

        for (ItemOperacaoNavio item : itens) {
            if (item.getOrdemTrabalhoPatioId() == null) {
                continue;
            }
            OrdemPatioYardRespostaDTO ordem = ordensPorItem.getOrDefault(
                    item.getId(),
                    ordensPorId.get(item.getOrdemTrabalhoPatioId()));
            if (ordem == null || !StringUtils.hasText(ordem.getStatusOrdem())) {
                continue;
            }
            StatusItemCarga statusAnterior = item.getStatus();
            StatusIntegracaoPatio integracaoAnterior = item.getStatusIntegracaoPatio();
            String posicaoRealAnterior = item.getPosicaoPatioReal();
            aplicarStatusYard(item, ordem);
            if (!Objects.equals(statusAnterior, item.getStatus())
                    || !Objects.equals(integracaoAnterior, item.getStatusIntegracaoPatio())
                    || !Objects.equals(posicaoRealAnterior, item.getPosicaoPatioReal())) {
                itemRepositorio.save(item);
                alterados++;
                visitaServico.registrarEvento(
                        visita,
                        item,
                        "STATUS_PATIO_SINCRONIZADO",
                        "Status do item sincronizado com a ordem real do patio.",
                        "sistema",
                        statusAnterior.name(),
                        item.getStatus().name());
            }
        }
        if (alterados > 0) {
            visitaServico.registrarEvento(
                    visita,
                    null,
                    "SINCRONIZACAO_PATIO_NAVIO",
                    alterados + " item(ns) reconciliado(s) entre patio e navio.",
                    "sistema",
                    null,
                    String.valueOf(alterados));
        }
        return alterados;
    }

    private void aplicarStatusYard(ItemOperacaoNavio item,
                                    OrdemPatioYardRespostaDTO ordem) {
        switch (ordem.getStatusOrdem()) {
            case "EM_EXECUCAO" -> {
                if (item.getStatus() != StatusItemCarga.OPERADO) {
                    item.setStatus(StatusItemCarga.EM_MOVIMENTO);
                }
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.EM_EXECUCAO);
            }
            case "CONCLUIDA" -> {
                item.setStatus(StatusItemCarga.OPERADO);
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.SINCRONIZADO);
                item.setPosicaoPatioReal(StringUtils.hasText(ordem.posicaoDestinoFormatada())
                        ? ordem.posicaoDestinoFormatada()
                        : item.getPosicaoPatioPlanejada());
                reservaPatioServico.consumirReservaDoItem(item, "sistema");
            }
            case "BLOQUEADA", "SUSPENSA" -> {
                if (item.getStatus() != StatusItemCarga.OPERADO) {
                    item.setStatus(StatusItemCarga.BLOQUEADO);
                }
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ERRO);
                item.setMotivoBloqueio(
                        "Ordem de patio " + ordem.getId()
                                + " em status " + ordem.getStatusOrdem());
            }
            case "CANCELADA" -> {
                if (item.getStatus() != StatusItemCarga.OPERADO) {
                    item.setStatus(StatusItemCarga.CANCELADO);
                }
                item.setStatusIntegracaoPatio(StatusIntegracaoPatio.CANCELADO);
                reservaPatioServico.cancelarReservaDoItem(
                        item,
                        "Ordem de patio cancelada.",
                        "sistema");
            }
            default -> item.setStatusIntegracaoPatio(StatusIntegracaoPatio.ORDEM_GERADA);
        }
    }
}
