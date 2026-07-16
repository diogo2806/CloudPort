package br.com.cloudport.servicoyard.edi.servico;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cloudport.runtime.jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EdiProcessamentoWorker {

    private static final Logger LOGGER = LogManager.getLogger(EdiProcessamentoWorker.class);

    private final EdiProcessamentoWorkerServico workerServico;
    private final EdiAuditoriaServico auditoriaServico;
    private final int tamanhoLote;

    public EdiProcessamentoWorker(
            EdiProcessamentoWorkerServico workerServico,
            EdiAuditoriaServico auditoriaServico,
            @Value("${cloudport.edi.worker.tamanho-lote:20}") int tamanhoLote) {
        this.workerServico = workerServico;
        this.auditoriaServico = auditoriaServico;
        this.tamanhoLote = Math.max(tamanhoLote, 1);
    }

    @Scheduled(fixedDelayString = "${cloudport.edi.worker.intervalo-ms:1000}")
    public void executar() {
        for (int indice = 0; indice < tamanhoLote; indice++) {
            try {
                if (!workerServico.processarProximo()) {
                    return;
                }
            } catch (FalhaProcessamentoEdiException ex) {
                registrarFalha(ex);
            } catch (RuntimeException ex) {
                LOGGER.error("Falha inesperada ao consultar a fila transacional EDI.", ex);
                return;
            }
        }
    }

    private void registrarFalha(FalhaProcessamentoEdiException ex) {
        try {
            auditoriaServico.registrarFalha(
                    ex.getProcessamentoId(),
                    ex.getCause() == null ? ex : ex.getCause(),
                    ex.isIrrecuperavel()
            );
        } catch (RuntimeException erroRegistro) {
            LOGGER.error(
                    "Nao foi possivel registrar a falha do processamento EDI {}.",
                    ex.getProcessamentoId(),
                    erroRegistro
            );
        }
    }
}
