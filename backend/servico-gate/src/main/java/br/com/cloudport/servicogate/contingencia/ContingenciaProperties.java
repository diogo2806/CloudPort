package br.com.cloudport.servicogate.contingencia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudport.gate.contingencia")
public class ContingenciaProperties {

    /**
     * Indica se as rotas de contingência devem ser expostas.
     */
    private boolean enabled;

    /**
     * Orientação operacional a ser exibida quando a contingência for acionada.
     */
    private String orientacaoOperador = "Acione o playbook de contingência do gate e mantenha registros manuais até a normalização do TOS.";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOrientacaoOperador() {
        return orientacaoOperador;
    }

    public void setOrientacaoOperador(String orientacaoOperador) {
        this.orientacaoOperador = orientacaoOperador;
    }
}
