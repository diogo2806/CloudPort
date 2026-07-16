package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.contracts.evento.EventoOperacaoPatioV1;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EventoOperacaoPatioPublicador {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventoOperacaoPatioPublicador.class);

    private final ApplicationEventPublisher eventPublisher;

    public EventoOperacaoPatioPublicador(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publicarWorkQueue(WorkQueuePatioRespostaDto fila,
                                  String tipoAlteracao,
                                  String correlationId) {
        if (fila == null || fila.getVisitaNavioId() == null) {
            LOGGER.debug("Evento do Yard nao publicado por ausencia da visita. tipo={}", tipoAlteracao);
            return;
        }
        publicar(fila.getVisitaNavioId(), fila.getId(), null, tipoAlteracao,
                null, fila.getStatus(), correlationId);
    }

    public void publicarInstrucao(OrdemTrabalhoPatioRespostaDto ordem,
                                   String tipoAlteracao,
                                   String correlationId) {
        if (ordem == null || ordem.getVisitaNavioId() == null) {
            LOGGER.debug("Evento do Yard nao publicado por ausencia da visita. tipo={}", tipoAlteracao);
            return;
        }
        String statusAtual = ordem.getStatusOrdem() == null ? null : ordem.getStatusOrdem().name();
        publicar(ordem.getVisitaNavioId(), ordem.getWorkQueueId(), ordem.getId(),
                tipoAlteracao, null, statusAtual, correlationId);
    }

    public void publicarDispatch(ResultadoDispatchWorkQueueDto resultado, String correlationId) {
        if (resultado == null || resultado.getTotalOrdensDespachadas() <= 0 || resultado.getOrdens() == null) {
            return;
        }
        OrdemTrabalhoPatioRespostaDto referencia = resultado.getOrdens().stream()
                .filter(Objects::nonNull)
                .filter(ordem -> ordem.getVisitaNavioId() != null)
                .findFirst()
                .orElse(null);
        if (referencia == null) {
            LOGGER.debug("Evento de dispatch nao publicado por ausencia da visita. workQueueId={}",
                    resultado.getWorkQueueId());
            return;
        }
        publicar(referencia.getVisitaNavioId(), resultado.getWorkQueueId(), null,
                "WORK_QUEUE_DESPACHADA", null, "EM_EXECUCAO", correlationId);
    }

    private void publicar(Long visitaNavioId,
                          Long workQueueId,
                          Long workInstructionId,
                          String tipoAlteracao,
                          String statusAnterior,
                          String statusAtual,
                          String correlationId) {
        eventPublisher.publishEvent(EventoOperacaoPatioV1.criar(
                visitaNavioId,
                workQueueId,
                workInstructionId,
                tipoAlteracao,
                statusAnterior,
                statusAtual,
                correlationId
        ));
    }
}
