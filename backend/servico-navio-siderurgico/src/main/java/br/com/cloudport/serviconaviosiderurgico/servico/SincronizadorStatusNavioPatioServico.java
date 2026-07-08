package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SincronizadorStatusNavioPatioServico {

    private final ItemOperacaoNavioRepositorio itemRepositorio;
    private final VisitaNavioServico visitaServico;

    public SincronizadorStatusNavioPatioServico(ItemOperacaoNavioRepositorio itemRepositorio, VisitaNavioServico visitaServico) {
        this.itemRepositorio = itemRepositorio;
        this.visitaServico = visitaServico;
    }

    @Transactional
    public int sincronizarStatus(Long visitaId) {
        var visita = visitaServico.buscarEntidade(visitaId);
        int alterados = 0;
        List<ItemOperacaoNavio> itens = itemRepositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId);
        for (ItemOperacaoNavio item : itens) {
            if (item.getOrdemTrabalhoPatioId() == null) {
                continue;
            }
            StatusItemCarga anterior = item.getStatus();
            if (item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.EM_EXECUCAO && item.getStatus() != StatusItemCarga.OPERADO) {
                item.setStatus(StatusItemCarga.EM_MOVIMENTO);
            } else if (item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.SINCRONIZADO) {
                item.setStatus(StatusItemCarga.OPERADO);
                item.setPosicaoPatioReal(item.getPosicaoPatioReal() == null ? item.getPosicaoPatioPlanejada() : item.getPosicaoPatioReal());
            } else if (item.getStatusIntegracaoPatio() == StatusIntegracaoPatio.CANCELADO && item.getStatus() != StatusItemCarga.OPERADO) {
                item.setStatus(StatusItemCarga.CANCELADO);
            }
            if (anterior != item.getStatus()) {
                itemRepositorio.save(item);
                alterados++;
                visitaServico.registrarEvento(visita, item, "STATUS_PATIO_SINCRONIZADO", "Status do item sincronizado com a ordem de patio.", "sistema", anterior.name(), item.getStatus().name());
            }
        }
        visitaServico.registrarEvento(visita, null, "SINCRONIZACAO_PATIO_NAVIO", alterados + " item(ns) reconciliado(s) entre patio e navio.", "sistema", null, String.valueOf(alterados));
        return alterados;
    }
}
