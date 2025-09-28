package br.com.cloudport.servicogate.dto;

import java.time.LocalDateTime;

public class GateEventDTO {

    private Long id;
    private String status;
    private String statusDescricao;
    private String motivoExcecao;
    private String motivoExcecaoDescricao;
    private String observacao;
    private String usuarioResponsavel;
    private LocalDateTime registradoEm;

    public GateEventDTO() {
    }

    public GateEventDTO(Long id, String status, String statusDescricao, String motivoExcecao,
                         String motivoExcecaoDescricao, String observacao, String usuarioResponsavel,
                         LocalDateTime registradoEm) {
        this.id = id;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.motivoExcecao = motivoExcecao;
        this.motivoExcecaoDescricao = motivoExcecaoDescricao;
        this.observacao = observacao;
        this.usuarioResponsavel = usuarioResponsavel;
        this.registradoEm = registradoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public void setStatusDescricao(String statusDescricao) {
        this.statusDescricao = statusDescricao;
    }

    public String getMotivoExcecao() {
        return motivoExcecao;
    }

    public void setMotivoExcecao(String motivoExcecao) {
        this.motivoExcecao = motivoExcecao;
    }

    public String getMotivoExcecaoDescricao() {
        return motivoExcecaoDescricao;
    }

    public void setMotivoExcecaoDescricao(String motivoExcecaoDescricao) {
        this.motivoExcecaoDescricao = motivoExcecaoDescricao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public void setUsuarioResponsavel(String usuarioResponsavel) {
        this.usuarioResponsavel = usuarioResponsavel;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }
}
