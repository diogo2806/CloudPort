package br.com.cloudport.servicogate.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudport.gate.flow")
public class GateFlowProperties {

    private Duration toleranciaEntradaAntecipada = Duration.ofMinutes(30);
    private Duration toleranciaEntradaAtraso = Duration.ofMinutes(30);
    private Duration toleranciaSaidaAntecipada = Duration.ofMinutes(30);
    private Duration toleranciaSaidaAtraso = Duration.ofMinutes(30);
    private List<String> rolesLiberacaoManual = new ArrayList<>();

    public Duration getToleranciaEntradaAntecipada() {
        return toleranciaEntradaAntecipada;
    }

    public void setToleranciaEntradaAntecipada(Duration toleranciaEntradaAntecipada) {
        this.toleranciaEntradaAntecipada = toleranciaEntradaAntecipada;
    }

    public Duration getToleranciaEntradaAtraso() {
        return toleranciaEntradaAtraso;
    }

    public void setToleranciaEntradaAtraso(Duration toleranciaEntradaAtraso) {
        this.toleranciaEntradaAtraso = toleranciaEntradaAtraso;
    }

    public Duration getToleranciaSaidaAntecipada() {
        return toleranciaSaidaAntecipada;
    }

    public void setToleranciaSaidaAntecipada(Duration toleranciaSaidaAntecipada) {
        this.toleranciaSaidaAntecipada = toleranciaSaidaAntecipada;
    }

    public Duration getToleranciaSaidaAtraso() {
        return toleranciaSaidaAtraso;
    }

    public void setToleranciaSaidaAtraso(Duration toleranciaSaidaAtraso) {
        this.toleranciaSaidaAtraso = toleranciaSaidaAtraso;
    }

    public List<String> getRolesLiberacaoManual() {
        return rolesLiberacaoManual;
    }

    public void setRolesLiberacaoManual(List<String> rolesLiberacaoManual) {
        this.rolesLiberacaoManual = rolesLiberacaoManual;
    }
}
