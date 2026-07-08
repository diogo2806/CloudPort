package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;

public record OrdemPatioDaVisitaDTO(
        Long id,
        Long visitaNavioId,
        Long itemOperacaoNavioId,
        String codigoLote,
        TipoMovimentoNavio tipoMovimento,
        String statusOrdem,
        String origem,
        String destino,
        String posicaoPlanejada,
        String posicaoReal,
        Integer sequenciaNavio,
        Integer prioridadeOperacional
) {
    public static OrdemPatioDaVisitaDTO de(ItemOperacaoNavio item) {
        return new OrdemPatioDaVisitaDTO(
                item.getOrdemTrabalhoPatioId(),
                item.getVisitaNavio().getId(),
                item.getId(),
                item.getCodigoLote(),
                item.getTipoMovimento(),
                statusOrdem(item),
                origem(item),
                destino(item),
                item.getPosicaoPatioPlanejada(),
                item.getPosicaoPatioReal(),
                item.getSequenciaOperacional(),
                item.getSequenciaOperacional()
        );
    }

    private static String origem(ItemOperacaoNavio item) {
        if (item.getTipoMovimento() == TipoMovimentoNavio.EMBARQUE) {
            return item.getOrigemPatio();
        }
        return "NAVIO";
    }

    private static String destino(ItemOperacaoNavio item) {
        if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA) {
            return item.getDestinoPatio() != null ? item.getDestinoPatio() : item.getPosicaoPatioPlanejada();
        }
        if (item.getTipoMovimento() == TipoMovimentoNavio.RESTOW) {
            return item.getPosicaoPlanejada();
        }
        return "NAVIO";
    }

    private static String statusOrdem(ItemOperacaoNavio item) {
        if (item.getStatus() == StatusItemCarga.OPERADO) {
            return "CONCLUIDA";
        }
        if (item.getStatus() == StatusItemCarga.EM_MOVIMENTO) {
            return "EM_EXECUCAO";
        }
        if (item.getStatus() == StatusItemCarga.BLOQUEADO) {
            return "BLOQUEADA";
        }
        if (item.getStatus() == StatusItemCarga.CANCELADO) {
            return "CANCELADA";
        }
        return "PENDENTE";
    }
}
