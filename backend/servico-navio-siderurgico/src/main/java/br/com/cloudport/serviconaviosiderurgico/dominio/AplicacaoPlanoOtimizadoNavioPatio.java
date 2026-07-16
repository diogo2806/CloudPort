package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "aplicacao_plano_otimizado_navio_patio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aplicacao_plano_otimizado_navio_patio",
                columnNames = {"plano_id", "visita_navio_id"}
        )
)
public class AplicacaoPlanoOtimizadoNavioPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plano_id", nullable = false, length = 100)
    private String planoId;

    @Column(name = "visita_navio_id", nullable = false)
    private Long visitaNavioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAplicacaoPlanoOtimizadoNavioPatio status;

    @Column(name = "resultado_json", columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(name = "erro", length = 2000)
    private String erro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public String getPlanoId() {
        return planoId;
    }

    public void setPlanoId(String planoId) {
        this.planoId = planoId;
    }

    public Long getVisitaNavioId() {
        return visitaNavioId;
    }

    public void setVisitaNavioId(Long visitaNavioId) {
        this.visitaNavioId = visitaNavioId;
    }

    public StatusAplicacaoPlanoOtimizadoNavioPatio getStatus() {
        return status;
    }

    public void setStatus(StatusAplicacaoPlanoOtimizadoNavioPatio status) {
        this.status = status;
    }

    public String getResultadoJson() {
        return resultadoJson;
    }

    public void setResultadoJson(String resultadoJson) {
        this.resultadoJson = resultadoJson;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
