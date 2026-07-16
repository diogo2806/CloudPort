package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.servico.ControlRoomStreamServico;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/visitas-navio")
public class ControlRoomStreamControlador {

    private final VisitaNavioServico visitaNavioServico;
    private final ControlRoomStreamServico controlRoomStreamServico;

    public ControlRoomStreamControlador(VisitaNavioServico visitaNavioServico,
                                         ControlRoomStreamServico controlRoomStreamServico) {
        this.visitaNavioServico = visitaNavioServico;
        this.controlRoomStreamServico = controlRoomStreamServico;
    }

    @GetMapping(path = "/{visitaId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter acompanhar(@PathVariable Long visitaId) {
        visitaNavioServico.buscarEntidade(visitaId);
        return controlRoomStreamServico.assinar(visitaId);
    }
}
