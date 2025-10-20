package br.com.cloudport.servicogate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudport.gate.ocr")
public class OcrIntegrationProperties {

    private String solicitacaoQueue = "gate.ocr.solicitacoes";
    private Duration tempoMaximoProcessamento = Duration.ofSeconds(10);

    public String getSolicitacaoQueue() {
        return solicitacaoQueue;
    }

    public void setSolicitacaoQueue(String solicitacaoQueue) {
        this.solicitacaoQueue = solicitacaoQueue;
    }

    public Duration getTempoMaximoProcessamento() {
        return tempoMaximoProcessamento;
    }

    public void setTempoMaximoProcessamento(Duration tempoMaximoProcessamento) {
        this.tempoMaximoProcessamento = tempoMaximoProcessamento;
    }
}
