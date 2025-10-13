package br.com.cloudport.servicogate.service.notification;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void enviarStatusAtualizado(Agendamento agendamento) {
        LOGGER.info("event=gate.status.atualizado agendamento={} status={}",
                agendamento.getCodigo(), agendamento.getStatus());
    }

    @Override
    public void enviarJanelaProxima(Agendamento agendamento, Duration antecedencia) {
        LOGGER.info("event=gate.janela.proxima agendamento={} antecedenciaMinutos={}",
                agendamento.getCodigo(), antecedencia != null ? antecedencia.toMinutes() : null);
    }

    @Override
    public void enviarDocumentosRevalidados(Agendamento agendamento, List<DocumentoAgendamento> documentos) {
        LOGGER.info("event=gate.documentos.revalidados agendamento={} totalDocumentos={}",
                agendamento.getCodigo(), documentos != null ? documentos.size() : 0);
    }
}
