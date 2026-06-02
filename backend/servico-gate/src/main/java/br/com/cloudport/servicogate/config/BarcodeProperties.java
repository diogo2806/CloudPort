package br.com.cloudport.servicogate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gate.barcode")
public class BarcodeProperties {

    private boolean habilitado = false;
    private Duration timeoutConfirmacao = Duration.ofSeconds(30);
    private boolean falharSemConfirmacao = false;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public Duration getTimeoutConfirmacao() {
        return timeoutConfirmacao;
    }

    public void setTimeoutConfirmacao(Duration timeoutConfirmacao) {
        this.timeoutConfirmacao = timeoutConfirmacao;
    }

    public boolean isFalharSemConfirmacao() {
        return falharSemConfirmacao;
    }

    public void setFalharSemConfirmacao(boolean falharSemConfirmacao) {
        this.falharSemConfirmacao = falharSemConfirmacao;
    }
}
