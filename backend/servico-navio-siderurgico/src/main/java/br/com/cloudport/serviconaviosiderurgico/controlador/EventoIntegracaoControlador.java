package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.servico.EventoIntegracaoPublicador;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class EventoIntegracaoControlador {

    private final EventoIntegracaoPublicador publicador;
    private final VisitaNavioServico visitaNavioServico;

    public EventoIntegracaoControlador(EventoIntegracaoPublicador publicador,
                                        VisitaNavioServico visitaNavioServico) {
        this.publicador = publicador;
        this.visitaNavioServico = visitaNavioServico;
    }

    @GetMapping(path = "/api/public/v1/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assinarEventosPublicos(@RequestParam(required = false) Long visitaId) {
        if (visitaId != null) {
            visitaNavioServico.buscarEntidade(visitaId);
        }
        return publicador.assinar(visitaId);
    }

    @GetMapping(path = "/visitas-navio/{visitaId}/eventos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assinarEventosDaVisita(@PathVariable Long visitaId) {
        visitaNavioServico.buscarEntidade(visitaId);
        return publicador.assinar(visitaId);
    }
}
