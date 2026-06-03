package br.com.cloudport.servicoyard.vesselplanner.modelo;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estivagem_plan")
public class EstivagemPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bay_plan_id")
    private Long bayPlanId;

    @Column(name = "codigo_navio", nullable = false, length = 50)
    private String codigoNavio;

    @Column(name = "codigo_viagem", nullable = false, length = 30)
    private String codigoViagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEstivagemPlan status;

    @Column(name = "comprimento_lpp")
    private Double comprimentoLpp;

    @Column
    private Double boca;

    @Column
    private Double calado;

    @Column
    private Double deslocamento;

    @Column
    private Double gm;

    @Column
    private Double tpc;

    @Column
    private Double lcb;

    @Column(name = "trim_calculado")
    private Double trimCalculado;

    @Column(name = "list_calculado")
    private Double listCalculado;

    @Column(name = "lcg_calculado")
    private Double lcgCalculado;

    @Column(name = "tcg_calculado")
    private Double tcgCalculado;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "estivagem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SlotNavio> slots = new ArrayList<>();

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public EstivagemPlan() {
        this.status = StatusEstivagemPlan.RASCUNHO;
        this.comprimentoLpp = 300.0;
        this.boca = 45.0;
        this.calado = 14.0;
        this.deslocamento = 90000.0;
        this.gm = 1.5;
        this.tpc = 75.0;
        this.lcb = 150.0;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBayPlanId() {
        return bayPlanId;
    }

    public void setBayPlanId(Long bayPlanId) {
        this.bayPlanId = bayPlanId;
    }

    public String getCodigoNavio() {
        return codigoNavio;
    }

    public void setCodigoNavio(String codigoNavio) {
        this.codigoNavio = codigoNavio;
    }

    public String getCodigoViagem() {
        return codigoViagem;
    }

    public void setCodigoViagem(String codigoViagem) {
        this.codigoViagem = codigoViagem;
    }

    public StatusEstivagemPlan getStatus() {
        return status;
    }

    public void setStatus(StatusEstivagemPlan status) {
        this.status = status;
    }

    public Double getComprimentoLpp() {
        return comprimentoLpp;
    }

    public void setComprimentoLpp(Double comprimentoLpp) {
        this.comprimentoLpp = comprimentoLpp;
    }

    public Double getBoca() {
        return boca;
    }

    public void setBoca(Double boca) {
        this.boca = boca;
    }

    public Double getCalado() {
        return calado;
    }

    public void setCalado(Double calado) {
        this.calado = calado;
    }

    public Double getDeslocamento() {
        return deslocamento;
    }

    public void setDeslocamento(Double deslocamento) {
        this.deslocamento = deslocamento;
    }

    public Double getGm() {
        return gm;
    }

    public void setGm(Double gm) {
        this.gm = gm;
    }

    public Double getTpc() {
        return tpc;
    }

    public void setTpc(Double tpc) {
        this.tpc = tpc;
    }

    public Double getLcb() {
        return lcb;
    }

    public void setLcb(Double lcb) {
        this.lcb = lcb;
    }

    public Double getTrimCalculado() {
        return trimCalculado;
    }

    public void setTrimCalculado(Double trimCalculado) {
        this.trimCalculado = trimCalculado;
    }

    public Double getListCalculado() {
        return listCalculado;
    }

    public void setListCalculado(Double listCalculado) {
        this.listCalculado = listCalculado;
    }

    public Double getLcgCalculado() {
        return lcgCalculado;
    }

    public void setLcgCalculado(Double lcgCalculado) {
        this.lcgCalculado = lcgCalculado;
    }

    public Double getTcgCalculado() {
        return tcgCalculado;
    }

    public void setTcgCalculado(Double tcgCalculado) {
        this.tcgCalculado = tcgCalculado;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public List<SlotNavio> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotNavio> slots) {
        this.slots = slots;
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
