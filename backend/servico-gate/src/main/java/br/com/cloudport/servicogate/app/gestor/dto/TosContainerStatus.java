package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;

public class TosContainerStatus {

    private final String containerNumber;
    private final String status;
    private final boolean gateLiberado;
    private final boolean liberacaoAduaneira;
    private final LocalDateTime ultimaAtualizacao;
    private final String motivoRestricao;

    public TosContainerStatus(String containerNumber,
                              String status,
                              boolean gateLiberado,
                              boolean liberacaoAduaneira,
                              LocalDateTime ultimaAtualizacao,
                              String motivoRestricao) {
        this.containerNumber = containerNumber;
        this.status = status;
        this.gateLiberado = gateLiberado;
        this.liberacaoAduaneira = liberacaoAduaneira;
        this.ultimaAtualizacao = ultimaAtualizacao;
        this.motivoRestricao = motivoRestricao;
    }

    public String getContainerNumber() {
        return containerNumber;
    }

    public String getStatus() {
        return status;
    }

    public boolean isGateLiberado() {
        return gateLiberado;
    }

    public boolean isLiberacaoAduaneira() {
        return liberacaoAduaneira;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public String getMotivoRestricao() {
        return motivoRestricao;
    }
}
