package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import java.time.Duration;
import java.util.List;

public interface NotificationGateway {

    void enviarStatusAtualizado(Agendamento agendamento);

    void enviarJanelaProxima(Agendamento agendamento, Duration antecedencia);

    void enviarDocumentosRevalidados(Agendamento agendamento, List<DocumentoAgendamento> documentos);
}
