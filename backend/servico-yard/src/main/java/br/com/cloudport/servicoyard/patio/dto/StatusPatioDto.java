package br.com.cloudport.servicoyard.patio.dto;

import java.time.LocalDateTime;

public class StatusPatioDto {

    private String status;
    private String descricao;
    private LocalDateTime verificadoEm;

    public StatusPatioDto() {
    }

    public StatusPatioDto(String status, String descricao, LocalDateTime verificadoEm) {
        this.status = status;
        this.descricao = descricao;
        this.verificadoEm = verificadoEm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getVerificadoEm() {
        return verificadoEm;
    }

    public void setVerificadoEm(LocalDateTime verificadoEm) {
        this.verificadoEm = verificadoEm;
    }
}
