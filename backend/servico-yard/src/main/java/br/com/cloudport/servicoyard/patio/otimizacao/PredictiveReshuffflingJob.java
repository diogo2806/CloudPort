package br.com.cloudport.servicoyard.patio.otimizacao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cloudport.runtime",
        name = "jobs-enabled",
        havingValue = "true")
public class PredictiveReshuffflingJob {

    private final PredictiveReshuffflingServico predictiveReshuffflingServico;

    public PredictiveReshuffflingJob(PredictiveReshuffflingServico predictiveReshuffflingServico) {
        this.predictiveReshuffflingServico = predictiveReshuffflingServico;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void executar() {
        predictiveReshuffflingServico.executarReshuffflingNoturno();
    }
}
