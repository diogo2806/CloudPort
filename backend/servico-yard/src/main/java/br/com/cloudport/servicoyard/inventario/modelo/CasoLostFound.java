package br.com.cloudport.servicoyard.inventario.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "lost_found_case")
public class CasoLostFound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificacao_lida", nullable = false, length = 40)
    private String identificacaoLida;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unidade_id")
    private UnidadeInventario unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_caso", nullable = false, length = 30)
    private TipoCasoLostFound tipoCaso;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusCasoLostFound status;

    @Column(name = "responsavel", length = 120)
    private String responsavel;

    @Column(name = "evidencia", length = 2000)
    private String evidencia;

    @Column(name = "decisao_final", length = 1000)
    private String decisaoFinal;

    @Column(name = "aberto_por", nullable = false, length = 120)
    private String abertoPor;

    @Column(name = "aberto_em", nullable = false)
    private LocalDateTime abertoEm;

    @Column(name = "investigacao_iniciada_em")
    private LocalDateTime investigacaoIniciadaEm;

    @Column(name = "associada_em")
    private LocalDateTime associadaEm;

    @Column(name = "regularizada_em")
    private LocalDateTime regularizadaEm;

    @Column(name = "baixada_em")
    private LocalDateTime baixadaEm;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void criarAuditoria() {
        LocalDateTime agora = LocalDateTime.now();
        if (abertoEm == null) {
            abertoEm = agora;
        }
        updatedAt = agora;
    }

    @PreUpdate
    public void atualizarAuditoria() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getIdentificacaoLida() { return identificacaoLida; }
    public void setIdentificacaoLida(String identificacaoLida) { this.identificacaoLida = identificacaoLida; }
    public UnidadeInventario getUnidade() { return unidade; }
    public void setUnidade(UnidadeInventario unidade) { this.unidade = unidade; }
    public TipoCasoLostFound getTipoCaso() { return tipoCaso; }
    public void setTipoCaso(TipoCasoLostFound tipoCaso) { this.tipoCaso = tipoCaso; }
    public StatusCasoLostFound getStatus() { return status; }
    public void setStatus(StatusCasoLostFound status) { this.status = status; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getDecisaoFinal() { return decisaoFinal; }
    public void setDecisaoFinal(String decisaoFinal) { this.decisaoFinal = decisaoFinal; }
    public String getAbertoPor() { return abertoPor; }
    public void setAbertoPor(String abertoPor) { this.abertoPor = abertoPor; }
    public LocalDateTime getAbertoEm() { return abertoEm; }
    public LocalDateTime getInvestigacaoIniciadaEm() { return investigacaoIniciadaEm; }
    public void setInvestigacaoIniciadaEm(LocalDateTime investigacaoIniciadaEm) { this.investigacaoIniciadaEm = investigacaoIniciadaEm; }
    public LocalDateTime getAssociadaEm() { return associadaEm; }
    public void setAssociadaEm(LocalDateTime associadaEm) { this.associadaEm = associadaEm; }
    public LocalDateTime getRegularizadaEm() { return regularizadaEm; }
    public void setRegularizadaEm(LocalDateTime regularizadaEm) { this.regularizadaEm = regularizadaEm; }
    public LocalDateTime getBaixadaEm() { return baixadaEm; }
    public void setBaixadaEm(LocalDateTime baixadaEm) { this.baixadaEm = baixadaEm; }
    public LocalDateTime getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(LocalDateTime encerradaEm) { this.encerradaEm = encerradaEm; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
