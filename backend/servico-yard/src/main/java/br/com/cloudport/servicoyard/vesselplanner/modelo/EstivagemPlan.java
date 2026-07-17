package br.com.cloudport.servicoyard.vesselplanner.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "estivagem_plan")
public class EstivagemPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "bay_plan_id") private Long bayPlanId;
    @Column(name = "navio_cadastro_id") private Long navioCadastroId;
    @Column(name = "visita_navio_id") private Long visitaNavioId;
    @Column(name = "codigo_visita", length = 60) private String codigoVisita;
    @Column(name = "versao_navio_canonico") private Long versaoNavioCanonico;
    @Column(name = "versao_visita") private Long versaoVisita;
    @Column(name = "codigo_navio", nullable = false, length = 50) private String codigoNavio;
    @Column(name = "codigo_viagem", nullable = false, length = 30) private String codigoViagem;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusEstivagemPlan status;
    @Column(name = "comprimento_lpp") private Double comprimentoLpp;
    @Column private Double boca; @Column private Double calado; @Column private Double deslocamento;
    @Column private Double gm; @Column private Double tpc; @Column private Double lcb;
    @Column(name = "trim_calculado") private Double trimCalculado;
    @Column(name = "list_calculado") private Double listCalculado;
    @Column(name = "lcg_calculado") private Double lcgCalculado;
    @Column(name = "tcg_calculado") private Double tcgCalculado;
    @Version private Long versao;
    @OneToMany(mappedBy = "estivagem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SlotNavio> slots = new ArrayList<>();
    @Column(name = "criado_em") private LocalDateTime criadoEm;
    @Column(name = "atualizado_em") private LocalDateTime atualizadoEm;
    public EstivagemPlan() {
        status = StatusEstivagemPlan.RASCUNHO; comprimentoLpp = 300.0; boca = 45.0; calado = 14.0;
        deslocamento = 90000.0; gm = 1.5; tpc = 75.0; lcb = 150.0;
    }
    @PrePersist @PreUpdate void touch() { atualizadoEm = LocalDateTime.now(); if (criadoEm == null) criadoEm = atualizadoEm; }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getBayPlanId() { return bayPlanId; } public void setBayPlanId(Long v) { bayPlanId = v; }
    public Long getNavioCadastroId() { return navioCadastroId; } public void setNavioCadastroId(Long v) { navioCadastroId = v; }
    public Long getVisitaNavioId() { return visitaNavioId; } public void setVisitaNavioId(Long v) { visitaNavioId = v; }
    public String getCodigoVisita() { return codigoVisita; } public void setCodigoVisita(String v) { codigoVisita = v; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; } public void setVersaoNavioCanonico(Long v) { versaoNavioCanonico = v; }
    public Long getVersaoVisita() { return versaoVisita; } public void setVersaoVisita(Long v) { versaoVisita = v; }
    public String getCodigoNavio() { return codigoNavio; } public void setCodigoNavio(String v) { codigoNavio = v; }
    public String getCodigoViagem() { return codigoViagem; } public void setCodigoViagem(String v) { codigoViagem = v; }
    public StatusEstivagemPlan getStatus() { return status; } public void setStatus(StatusEstivagemPlan v) { status = v; }
    public Double getComprimentoLpp() { return comprimentoLpp; } public void setComprimentoLpp(Double v) { comprimentoLpp = v; }
    public Double getBoca() { return boca; } public void setBoca(Double v) { boca = v; }
    public Double getCalado() { return calado; } public void setCalado(Double v) { calado = v; }
    public Double getDeslocamento() { return deslocamento; } public void setDeslocamento(Double v) { deslocamento = v; }
    public Double getGm() { return gm; } public void setGm(Double v) { gm = v; }
    public Double getTpc() { return tpc; } public void setTpc(Double v) { tpc = v; }
    public Double getLcb() { return lcb; } public void setLcb(Double v) { lcb = v; }
    public Double getTrimCalculado() { return trimCalculado; } public void setTrimCalculado(Double v) { trimCalculado = v; }
    public Double getListCalculado() { return listCalculado; } public void setListCalculado(Double v) { listCalculado = v; }
    public Double getLcgCalculado() { return lcgCalculado; } public void setLcgCalculado(Double v) { lcgCalculado = v; }
    public Double getTcgCalculado() { return tcgCalculado; } public void setTcgCalculado(Double v) { tcgCalculado = v; }
    public Long getVersao() { return versao; } public void setVersao(Long v) { versao = v; }
    public List<SlotNavio> getSlots() { return slots; } public void setSlots(List<SlotNavio> v) { slots = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; } public void setCriadoEm(LocalDateTime v) { criadoEm = v; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; } public void setAtualizadoEm(LocalDateTime v) { atualizadoEm = v; }
}
