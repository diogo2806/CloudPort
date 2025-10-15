package br.com.cloudport.servicogate.app.administracao.dto;

public class ContingenciaResponse {

    private final String protocolo;
    private final String orientacoes;

    public ContingenciaResponse(String protocolo, String orientacoes) {
        this.protocolo = protocolo;
        this.orientacoes = orientacoes;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public String getOrientacoes() {
        return orientacoes;
    }
}
