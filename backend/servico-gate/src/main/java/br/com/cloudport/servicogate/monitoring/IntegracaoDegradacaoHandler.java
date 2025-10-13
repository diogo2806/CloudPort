package br.com.cloudport.servicogate.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IntegracaoDegradacaoHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegracaoDegradacaoHandler.class);

    private final GateMetrics gateMetrics;

    public IntegracaoDegradacaoHandler(GateMetrics gateMetrics) {
        this.gateMetrics = gateMetrics;
    }

    public void registrarDegradacao(String sistema, String origem, String orientacaoOperador) {
        gateMetrics.registrarEventoDegradacao(sistema, origem);
        LOGGER.warn("event=integracao.degradada sistema={} origem={} orientacao=\"{}\"", sistema, origem, orientacaoOperador);
    }
}
