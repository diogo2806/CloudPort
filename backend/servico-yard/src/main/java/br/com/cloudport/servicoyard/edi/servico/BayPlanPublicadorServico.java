package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.AtualizacaoBayPlanEventoDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica atualizações de Bay Plan em tempo real via WebSocket STOMP.
 * Tópico: /topico/edi/bay-plan/{codigoNavio}
 *
 * O frontend pode se inscrever em /topico/edi/bay-plan/MSC_GULSEUM
 * e receber o delta sempre que um COPRAR ou COARRI chegar.
 */
@Service
public class BayPlanPublicadorServico {

    private static final String TOPICO_BASE = "/topico/edi/bay-plan/";

    private final SimpMessagingTemplate messagingTemplate;

    public BayPlanPublicadorServico(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publicarCriacaoBaplie(BayPlan bayPlan) {
        AtualizacaoBayPlanEventoDto evento = new AtualizacaoBayPlanEventoDto();
        evento.setBayPlanId(bayPlan.getId());
        evento.setCodigoNavio(bayPlan.getCodigoNavio());
        evento.setCodigoViagem(bayPlan.getCodigoViagem());
        evento.setTipoMensagem(TipoMensagemEdi.BAPLIE);
        evento.setNovoStatus(bayPlan.getStatus());
        evento.setContainersAdicionados(bayPlan.getContainers().stream()
                .map(c -> c.getCodigoContainer())
                .collect(Collectors.toList()));
        evento.setTotalContainers(bayPlan.getContainers().size());
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    public void publicarAtualizacaoCoprar(BayPlan bayPlan,
                                          List<String> adicionados,
                                          List<String> atualizados) {
        AtualizacaoBayPlanEventoDto evento = new AtualizacaoBayPlanEventoDto();
        evento.setBayPlanId(bayPlan.getId());
        evento.setCodigoNavio(bayPlan.getCodigoNavio());
        evento.setCodigoViagem(bayPlan.getCodigoViagem());
        evento.setTipoMensagem(TipoMensagemEdi.COPRAR);
        evento.setNovoStatus(bayPlan.getStatus());
        evento.setContainersAdicionados(adicionados);
        evento.setContainersAtualizados(atualizados);
        evento.setTotalContainers(bayPlan.getContainers().size());
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    public void publicarConfirmacaoCoarri(BayPlan bayPlan, List<String> confirmados) {
        AtualizacaoBayPlanEventoDto evento = new AtualizacaoBayPlanEventoDto();
        evento.setBayPlanId(bayPlan.getId());
        evento.setCodigoNavio(bayPlan.getCodigoNavio());
        evento.setCodigoViagem(bayPlan.getCodigoViagem());
        evento.setTipoMensagem(TipoMensagemEdi.COARRI);
        evento.setNovoStatus(bayPlan.getStatus());
        evento.setContainersAtualizados(confirmados);
        evento.setTotalContainers(bayPlan.getContainers().size());
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    private void publicar(String codigoNavio, AtualizacaoBayPlanEventoDto evento) {
        messagingTemplate.convertAndSend(TOPICO_BASE + codigoNavio, evento);
    }
}
