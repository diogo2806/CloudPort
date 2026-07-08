package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.AlertaIntegracaoNavioPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ValidadorIntegracaoNavioPatioServico {

    private final ReservaPosicaoPatioNavioRepositorio reservaRepositorio;

    public ValidadorIntegracaoNavioPatioServico(ReservaPosicaoPatioNavioRepositorio reservaRepositorio) {
        this.reservaRepositorio = reservaRepositorio;
    }

    public List<String> validarGeracaoOrdem(ItemOperacaoNavio item) {
        List<String> erros = new ArrayList<>();
        if (item.getStatus() == StatusItemCarga.BLOQUEADO) {
            erros.add("Item bloqueado nao pode gerar ordem executavel.");
        }
        if (item.getStatus() == StatusItemCarga.OPERADO) {
            erros.add("Item ja operado nao pode gerar nova ordem automaticamente.");
        }
        if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA
                && !StringUtils.hasText(item.getDestinoPatio())
                && !StringUtils.hasText(item.getPosicaoPatioPlanejada())) {
            erros.add("Item de descarga sem destino/reserva de patio.");
        }
        if (item.getTipoMovimento() == TipoMovimentoNavio.EMBARQUE
                && !StringUtils.hasText(item.getOrigemPatio())
                && item.getConteinerPatioId() == null
                && item.getCargaPatioId() == null) {
            erros.add("Item de embarque sem origem de patio ou carga fisica localizada.");
        }
        return erros;
    }

    public List<AlertaIntegracaoNavioPatioDTO> listarAlertas(Long visitaId, List<ItemOperacaoNavio> itens) {
        List<AlertaIntegracaoNavioPatioDTO> alertas = new ArrayList<>();
        Set<Long> ordensAtivas = new HashSet<>();
        for (ItemOperacaoNavio item : itens) {
            boolean temReservaAtiva = reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                    item.getId(), List.of(StatusReservaPatioNavio.ATIVA)).isPresent();
            if (item.getTipoMovimento() == TipoMovimentoNavio.DESCARGA
                    && item.getStatus() != StatusItemCarga.OPERADO
                    && !temReservaAtiva
                    && !StringUtils.hasText(item.getPosicaoPatioPlanejada())) {
                alertas.add(alerta("ITEM_DESCARGA_SEM_RESERVA", "ALTA", visitaId, item, "Item de descarga sem destino ou reserva de patio."));
            }
            if (item.getTipoMovimento() == TipoMovimentoNavio.EMBARQUE
                    && !StringUtils.hasText(item.getOrigemPatio())
                    && item.getConteinerPatioId() == null
                    && item.getCargaPatioId() == null) {
                alertas.add(alerta("ITEM_EMBARQUE_SEM_ORIGEM", "ALTA", visitaId, item, "Item de embarque sem origem de patio."));
            }
            if (item.getOrdemTrabalhoPatioId() != null && !ordensAtivas.add(item.getOrdemTrabalhoPatioId())) {
                alertas.add(alerta("ORDEM_DUPLICADA_ATIVA", "CRITICA", visitaId, item, "Ordem de patio duplicada dentro da visita."));
            }
            if (item.getStatus() == StatusItemCarga.BLOQUEADO && item.getOrdemTrabalhoPatioId() != null) {
                alertas.add(alerta("ITEM_BLOQUEADO_COM_ORDEM", "ALTA", visitaId, item, "Item bloqueado possui ordem de patio vinculada."));
            }
            if (item.getStatus() == StatusItemCarga.OPERADO && item.getOrdemTrabalhoPatioId() == null && item.getMovimentoPatioId() == null) {
                alertas.add(alerta("ITEM_OPERADO_SEM_MOVIMENTO_PATIO", "MEDIA", visitaId, item, "Item operado sem ordem ou movimento de patio correspondente."));
            }
            if (temDivergenciaPosicao(item)) {
                alertas.add(alerta("DIVERGENCIA_POSICAO_PATIO", "MEDIA", visitaId, item, "Divergencia entre posicao de patio planejada e real."));
            }
            if (StringUtils.hasText(item.getPosicaoPatioPlanejada()) && item.getPosicaoPatioPlanejada().toUpperCase().contains("REHANDLE")) {
                alertas.add(alerta("REHANDLE_ACIMA_LIMITE", "MEDIA", visitaId, item, "Rehandle previsto deve ser revisado antes da execucao."));
            }
        }
        return alertas;
    }

    private boolean temDivergenciaPosicao(ItemOperacaoNavio item) {
        return StringUtils.hasText(item.getPosicaoPatioPlanejada())
                && StringUtils.hasText(item.getPosicaoPatioReal())
                && !Objects.equals(item.getPosicaoPatioPlanejada().trim().toUpperCase(), item.getPosicaoPatioReal().trim().toUpperCase());
    }

    private AlertaIntegracaoNavioPatioDTO alerta(String tipo, String severidade, Long visitaId, ItemOperacaoNavio item, String mensagem) {
        return new AlertaIntegracaoNavioPatioDTO(tipo, severidade, visitaId, item.getId(), item.getOrdemTrabalhoPatioId(), mensagem);
    }
}
