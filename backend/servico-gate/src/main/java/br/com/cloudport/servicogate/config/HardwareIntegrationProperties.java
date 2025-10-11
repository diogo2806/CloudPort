package br.com.cloudport.servicogate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudport.gate.hardware")
public class HardwareIntegrationProperties {

    private String entradaQueue = "gate.hardware.entrada";
    private String saidaQueue = "gate.hardware.saida";
    private String decisaoExchange = "gate.hardware.decisao";
    private String decisaoRoutingEntrada = "gate.hardware.decisao.entrada";
    private String decisaoRoutingSaida = "gate.hardware.decisao.saida";

    public String getEntradaQueue() {
        return entradaQueue;
    }

    public void setEntradaQueue(String entradaQueue) {
        this.entradaQueue = entradaQueue;
    }

    public String getSaidaQueue() {
        return saidaQueue;
    }

    public void setSaidaQueue(String saidaQueue) {
        this.saidaQueue = saidaQueue;
    }

    public String getDecisaoExchange() {
        return decisaoExchange;
    }

    public void setDecisaoExchange(String decisaoExchange) {
        this.decisaoExchange = decisaoExchange;
    }

    public String getDecisaoRoutingEntrada() {
        return decisaoRoutingEntrada;
    }

    public void setDecisaoRoutingEntrada(String decisaoRoutingEntrada) {
        this.decisaoRoutingEntrada = decisaoRoutingEntrada;
    }

    public String getDecisaoRoutingSaida() {
        return decisaoRoutingSaida;
    }

    public void setDecisaoRoutingSaida(String decisaoRoutingSaida) {
        this.decisaoRoutingSaida = decisaoRoutingSaida;
    }
}
