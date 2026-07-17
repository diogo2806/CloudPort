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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "perfil_geometria_navio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_perfil_geometria_navio_codigo_versao",
                columnNames = {"codigo_navio", "versao_perfil"}))
public class PerfilGeometriaNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_navio", nullable = false, length = 50)
    private String codigoNavio;

    @Column(name = "versao_perfil", nullable = false)
    private Long versaoPerfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPerfilGeometriaNavio status = StatusPerfilGeometriaNavio.RASCUNHO;

    @Column(name = "condicao_carregamento", nullable = false, length = 80)
    private String condicaoCarregamento;

    @Column(name = "comprimento_lpp", nullable = false)
    private Double comprimentoLpp;

    @Column(nullable = false)
    private Double boca;

    @Column(nullable = false)
    private Double calado;

    @Column(nullable = false)
    private Double deslocamento;

    @Column(nullable = false)
    private Double gm;

    @Column(nullable = false)
    private Double tpc;

    @Column(nullable = false)
    private Double lcb;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SlotPerfilNavio> slots = new ArrayList<>();

    @Version
    @Column(name = "versao_registro", nullable = false)
    private Long versaoRegistro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
    }

    public void adicionarSlot(SlotPerfilNavio slot) {
        slot.setPerfil(this);
        slots.add(slot);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public Long getVersaoPerfil() { return versaoPerfil; }
    public void setVersaoPerfil(Long versaoPerfil) { this.versaoPerfil = versaoPerfil; }
    public StatusPerfilGeometriaNavio getStatus() { return status; }
    public void setStatus(StatusPerfilGeometriaNavio status) { this.status = status; }
    public String getCondicaoCarregamento() { return condicaoCarregamento; }
    public void setCondicaoCarregamento(String condicaoCarregamento) { this.condicaoCarregamento = condicaoCarregamento; }
    public Double getComprimentoLpp() { return comprimentoLpp; }
    public void setComprimentoLpp(Double comprimentoLpp) { this.comprimentoLpp = comprimentoLpp; }
    public Double getBoca() { return boca; }
    public void setBoca(Double boca) { this.boca = boca; }
    public Double getCalado() { return calado; }
    public void setCalado(Double calado) { this.calado = calado; }
    public Double getDeslocamento() { return deslocamento; }
    public void setDeslocamento(Double deslocamento) { this.deslocamento = deslocamento; }
    public Double getGm() { return gm; }
    public void setGm(Double gm) { this.gm = gm; }
    public Double getTpc() { return tpc; }
    public void setTpc(Double tpc) { this.tpc = tpc; }
    public Double getLcb() { return lcb; }
    public void setLcb(Double lcb) { this.lcb = lcb; }
    public List<SlotPerfilNavio> getSlots() { return slots; }
    public void setSlots(List<SlotPerfilNavio> slots) { this.slots = slots; }
    public Long getVersaoRegistro() { return versaoRegistro; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
