package br.com.cloudport.servicoyard.edi.servico;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cloudport.edi.worker.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EdiProcessamentoWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdiProcessamentoWorker.class);

    private final EdiFilaProcessamentoServico filaServico;
    private final EdiExecucaoTransacionalServico execucaoServico;

    public EdiProcessamentoWorker(EdiFilaProcessamentoServico filaServico,
                                  EdiExecucaoTransacionalServico execucaoServico) {
        this.filaServico = filaServico;
        this.execucaoServico = execucaoServico;
    }

    @Scheduled(fixedDelayString = "${cloudport.edi.worker.intervalo-ms:1000}")
    public void processarPendentes() {
        int recuperados = filaServico.recuperarProcessamentosTravados();
        if (recuperados > 0) {
            LOGGER.warn("{} processamento(s) EDI interrompido(s) foram recolocados na fila.", recuperados);
        }

        List<Long> processamentos = filaServico.reivindicarPendentes();
        for (Long processamentoId : processamentos) {
            try {
                execucaoServico.executar(processamentoId);
                LOGGER.info("Processamento EDI concluido. processamentoId={}", processamentoId);
            } catch (RuntimeException ex) {
                filaServico.registrarFalha(processamentoId, ex);
                LOGGER.warn("Falha no processamento EDI {}: {}", processamentoId, ex.getMessage());
            }
        }
    }
}
