package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.AtualizacaoBayPlanEventoDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BayPlanPublicadorServico {

    private static final String TOPICO_BASE = "/topico/edi/bay-plan/";

    private final SimpMessagingTemplate messagingTemplate;

    public BayPlanPublicadorServico(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publicarCriacaoBaplie(BayPlan bayPlan) {
        AtualizacaoBayPlanEventoDto evento = eventoBase(bayPlan, TipoMensagemEdi.BAPLIE);
        evento.setContainersAdicionados(bayPlan.getContainers().stream()
                .map(c -> c.getCodigoContainer())
                .collect(Collectors.toList()));
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    public void publicarAtualizacaoCoprar(BayPlan bayPlan,
                                           List<String> adicionados,
                                           List<String> atualizados) {
        AtualizacaoBayPlanEventoDto evento = eventoBase(bayPlan, TipoMensagemEdi.COPRAR);
        evento.setContainersAdicionados(adicionados);
        evento.setContainersAtualizados(atualizados);
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    public void publicarConfirmacaoCoarri(BayPlan bayPlan, List<String> confirmados) {
        AtualizacaoBayPlanEventoDto evento = eventoBase(bayPlan, TipoMensagemEdi.COARRI);
        evento.setContainersAtualizados(confirmados);
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    public void publicarAtualizacaoVermas(BayPlan bayPlan, List<String> atualizados) {
        AtualizacaoBayPlanEventoDto evento = eventoBase(bayPlan, TipoMensagemEdi.VERMAS);
        evento.setContainersAtualizados(atualizados);
        publicar(bayPlan.getCodigoNavio(), evento);
    }

    private AtualizacaoBayPlanEventoDto eventoBase(BayPlan bayPlan, TipoMensagemEdi tipoMensagem) {
        AtualizacaoBayPlanEventoDto evento = new AtualizacaoBayPlanEventoDto();
        evento.setBayPlanId(bayPlan.getId());
        evento.setCodigoNavio(bayPlan.getCodigoNavio());
        evento.setCodigoViagem(bayPlan.getCodigoViagem());
        evento.setTipoMensagem(tipoMensagem);
        evento.setNovoStatus(bayPlan.getStatus());
        evento.setTotalContainers(bayPlan.getContainers().size());
        return evento;
    }

    private void publicar(String codigoNavio, AtualizacaoBayPlanEventoDto evento) {
        messagingTemplate.convertAndSend(TOPICO_BASE + codigoNavio, evento);
    }
}
