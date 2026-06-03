package br.com.cloudport.servicoyard.estivagembulk.modelo;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plano_estiva_bulk")
public class PlanoEstivaBulk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navio_granel_id")
    private NavioGranel navio;

    @Column(name = "codigo_viagem", length = 30)
    private String codigoViagem;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private StatusPlanoEstiva status;

    @Column(name = "bm_max_calculado")
    private Double bmMaxCalculado;

    @Column(name = "sf_max_calculado")
    private Double sfMaxCalculado;

    @Column(name = "trim_calculado")
    private Double trimCalculado;

    @Column(name = "list_calculado")
    private Double listCalculado;

    @Column(name = "calado_saida")
    private Double calado_saida;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BobinaManifesto> bobinas = new ArrayList<>();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PosicaoBobina> posicoes = new ArrayList<>();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<MaterialLashingBulk> materiais = new ArrayList<>();

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    public PlanoEstivaBulk() {
        this.status = StatusPlanoEstiva.RASCUNHO;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NavioGranel getNavio() {
        return navio;
    }

    public void setNavio(NavioGranel navio) {
        this.navio = navio;
    }

    public String getCodigoViagem() {
        return codigoViagem;
    }

    public void setCodigoViagem(String codigoViagem) {
        this.codigoViagem = codigoViagem;
    }

    public String getPortoCarga() {
        return portoCarga;
    }

    public void setPortoCarga(String portoCarga) {
        this.portoCarga = portoCarga;
    }

    public String getPortoDescarga() {
        return portoDescarga;
    }

    public void setPortoDescarga(String portoDescarga) {
        this.portoDescarga = portoDescarga;
    }

    public StatusPlanoEstiva getStatus() {
        return status;
    }

    public void setStatus(StatusPlanoEstiva status) {
        this.status = status;
    }

    public Double getBmMaxCalculado() {
        return bmMaxCalculado;
    }

    public void setBmMaxCalculado(Double bmMaxCalculado) {
        this.bmMaxCalculado = bmMaxCalculado;
    }

    public Double getSfMaxCalculado() {
        return sfMaxCalculado;
    }

    public void setSfMaxCalculado(Double sfMaxCalculado) {
        this.sfMaxCalculado = sfMaxCalculado;
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

    public Double getCalado_saida() {
        return calado_saida;
    }

    public void setCalado_saida(Double calado_saida) {
        this.calado_saida = calado_saida;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public List<BobinaManifesto> getBobinas() {
        return bobinas;
    }

    public void setBobinas(List<BobinaManifesto> bobinas) {
        this.bobinas = bobinas;
    }

    public List<PosicaoBobina> getPosicoes() {
        return posicoes;
    }

    public void setPosicoes(List<PosicaoBobina> posicoes) {
        this.posicoes = posicoes;
    }

    public List<MaterialLashingBulk> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<MaterialLashingBulk> materiais) {
        this.materiais = materiais;
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
