package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;

public class AlertaDTO {

    private Long id;
    private String tipo;
    private String severidade;
    private String entidadeId;
    private String descricao;
    private LocalDateTime dataGerada;
    private LocalDateTime dataResolucao;
    private String status;
    private String acaoSugerida;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getSeveridade() { return severidade; }
    public void setSeveridade(String severidade) { this.severidade = severidade; }

    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDateTime getDataGerada() { return dataGerada; }
    public void setDataGerada(LocalDateTime dataGerada) { this.dataGerada = dataGerada; }

    public LocalDateTime getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(LocalDateTime dataResolucao) { this.dataResolucao = dataResolucao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAcaoSugerida() { return acaoSugerida; }
    public void setAcaoSugerida(String acaoSugerida) { this.acaoSugerida = acaoSugerida; }
}