package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.evento.EventoCadastroNavioV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventoCadastroNavioListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventoCadastroNavioListener.class);

    private final ProcessamentoEventoInternoServico processamentoEventoServico;
    private final NavioSiderurgicoServico navioSiderurgicoServico;

    public EventoCadastroNavioListener(ProcessamentoEventoInternoServico processamentoEventoServico,
                                       NavioSiderurgicoServico navioSiderurgicoServico) {
        this.processamentoEventoServico = processamentoEventoServico;
        this.navioSiderurgicoServico = navioSiderurgicoServico;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void aoAlterarCadastroNavio(EventoCadastroNavioV1 evento) {
        boolean processado = processamentoEventoServico.processarUmaVez(
                evento.eventId(),
                "NAVIO_CANONICO:" + evento.tipoAlteracao(),
                () -> aplicar(evento)
        );
        if (processado) {
            LOGGER.debug("Evento do cadastro canônico aplicado. navioCadastroId={} eventoId={} tipo={}",
                    evento.navioCadastroId(), evento.eventId(), evento.tipoAlteracao());
        } else {
            LOGGER.debug("Redelivery de cadastro canônico ignorado. eventoId={}", evento.eventId());
        }
    }

    private void aplicar(EventoCadastroNavioV1 evento) {
        if ("REMOVIDO".equals(evento.tipoAlteracao())) {
            navioSiderurgicoServico.cancelarPorCadastroRemovido(evento.navioCadastroId());
            return;
        }
        navioSiderurgicoServico.sincronizarCadastroCanonico(evento.navioCadastroId());
    }
}
