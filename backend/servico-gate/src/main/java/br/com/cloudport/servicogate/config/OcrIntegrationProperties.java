package br.com.cloudport.servicogate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudport.gate.ocr")
public class OcrIntegrationProperties {

    private Duration tempoMaximoProcessamento = Duration.ofSeconds(10);

    public Duration getTempoMaximoProcessamento() {
        return tempoMaximoProcessamento;
    }

    public void setTempoMaximoProcessamento(Duration tempoMaximoProcessamento) {
        this.tempoMaximoProcessamento = tempoMaximoProcessamento;
    }
}
