package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.evento.EventoOperacaoPatioV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventoOperacaoPatioListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventoOperacaoPatioListener.class);

    private final ProcessamentoEventoInternoServico processamentoEventoServico;
    private final SincronizadorStatusNavioPatioServico sincronizadorStatus;

    public EventoOperacaoPatioListener(ProcessamentoEventoInternoServico processamentoEventoServico,
                                       SincronizadorStatusNavioPatioServico sincronizadorStatus) {
        this.processamentoEventoServico = processamentoEventoServico;
        this.sincronizadorStatus = sincronizadorStatus;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAlterarOperacaoPatio(EventoOperacaoPatioV1 evento) {
        boolean processado = processamentoEventoServico.processarUmaVez(
                evento.eventId(),
                "YARD:" + evento.tipoAlteracao(),
                () -> sincronizadorStatus.sincronizarStatus(evento.visitaNavioId())
        );
        if (processado) {
            LOGGER.debug("Evento do Yard aplicado a visita {}. eventoId={} tipo={}",
                    evento.visitaNavioId(), evento.eventId(), evento.tipoAlteracao());
        } else {
            LOGGER.debug("Redelivery de evento do Yard ignorado. eventoId={}", evento.eventId());
        }
    }
}
