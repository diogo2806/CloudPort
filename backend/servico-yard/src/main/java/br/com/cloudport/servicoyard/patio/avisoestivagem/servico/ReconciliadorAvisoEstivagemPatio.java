package br.com.cloudport.servicoyard.patio.avisoestivagem.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliadorAvisoEstivagemPatio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliadorAvisoEstivagemPatio.class);

    private final AvisoEstivagemPatioServico servico;

    public ReconciliadorAvisoEstivagemPatio(AvisoEstivagemPatioServico servico) {
        this.servico = servico;
    }

    @Scheduled(
            initialDelayString = "${cloudport.yard.avisos-estivagem.initial-delay-ms:15000}",
            fixedDelayString = "${cloudport.yard.avisos-estivagem.fixed-delay-ms:30000}")
    public void revalidarInventario() {
        try {
            servico.revalidarInventario("RECONCILIADOR_ESTIVAGEM_PATIO");
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao revalidar avisos de estivagem do pátio.", ex);
        }
    }
}
