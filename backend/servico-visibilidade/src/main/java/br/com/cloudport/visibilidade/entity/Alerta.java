package br.com.cloudport.visibilidade.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "severidade")
    private String severidade;

    @Column(name = "entidade_id")
    private String entidadeId;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data_gerada")
    private LocalDateTime dataGerada;

    @Column(name = "data_reconhecimento")
    private LocalDateTime dataReconhecimento;

    @Column(name = "reconhecido_por")
    private String reconhecidoPor;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @Column(name = "resolvido_por")
    private String resolvidoPor;

    @Column(name = "status")
    private String status;

    @Column(name = "acao_sugerida", columnDefinition = "TEXT")
    private String acaoSugerida;

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

    public LocalDateTime getDataReconhecimento() { return dataReconhecimento; }
    public void setDataReconhecimento(LocalDateTime dataReconhecimento) { this.dataReconhecimento = dataReconhecimento; }

    public String getReconhecidoPor() { return reconhecidoPor; }
    public void setReconhecidoPor(String reconhecidoPor) { this.reconhecidoPor = reconhecidoPor; }

    public LocalDateTime getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(LocalDateTime dataResolucao) { this.dataResolucao = dataResolucao; }

    public String getResolvidoPor() { return resolvidoPor; }
    public void setResolvidoPor(String resolvidoPor) { this.resolvidoPor = resolvidoPor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAcaoSugerida() { return acaoSugerida; }
    public void setAcaoSugerida(String acaoSugerida) { this.acaoSugerida = acaoSugerida; }
}
