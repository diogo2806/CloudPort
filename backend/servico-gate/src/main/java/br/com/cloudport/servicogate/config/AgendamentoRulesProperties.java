package br.com.cloudport.servicogate.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudport.gate.agendamento.rules")
public class AgendamentoRulesProperties {

    /**
     * Antecedência mínima necessária para criação de agendamentos em relação ao início da janela.
     */
    private Duration antecedenciaMinima = Duration.ofHours(2);

    /**
     * Tempo máximo após o início da janela em que ainda é permitido criar agendamentos.
     */
    private Duration atrasoMaximo = Duration.ofHours(1);

    /**
     * Período anterior ao início da janela em que edições são permitidas.
     */
    private Duration edicaoAntecedencia = Duration.ofHours(12);

    /**
     * Período posterior ao início da janela em que edições são permitidas.
     */
    private Duration edicaoAtraso = Duration.ofHours(2);

    public Duration getAntecedenciaMinima() {
        return antecedenciaMinima;
    }

    public void setAntecedenciaMinima(Duration antecedenciaMinima) {
        this.antecedenciaMinima = antecedenciaMinima;
    }

    public Duration getAtrasoMaximo() {
        return atrasoMaximo;
    }

    public void setAtrasoMaximo(Duration atrasoMaximo) {
        this.atrasoMaximo = atrasoMaximo;
    }

    public Duration getEdicaoAntecedencia() {
        return edicaoAntecedencia;
    }

    public void setEdicaoAntecedencia(Duration edicaoAntecedencia) {
        this.edicaoAntecedencia = edicaoAntecedencia;
    }

    public Duration getEdicaoAtraso() {
        return edicaoAtraso;
    }

    public void setEdicaoAtraso(Duration edicaoAtraso) {
        this.edicaoAtraso = edicaoAtraso;
    }
}
