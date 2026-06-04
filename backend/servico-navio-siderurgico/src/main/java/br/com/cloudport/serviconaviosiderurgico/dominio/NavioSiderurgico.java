package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "navio_siderurgico")
public class NavioSiderurgico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "codigo_imo", nullable = false, unique = true, length = 10)
    private String codigoImo;

    @Column(name = "pais_bandeira", nullable = false, length = 60)
    private String paisBandeira;

    @Column(name = "empresa_armadora", nullable = false, length = 80)
    private String empresaArmadora;

    @Column(name = "tipo_navio", nullable = false, length = 40)
    private String tipoNavio;

    @Column(name = "loa_metros", precision = 8, scale = 2)
    private BigDecimal loaMetros;

    @Column(name = "dwt_toneladas", precision = 12, scale = 2)
    private BigDecimal dwtToneladas;

    @Column(name = "quantidade_poroes", nullable = false)
    private Integer quantidadePoroes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusNavioSiderurgico status = StatusNavioSiderurgico.PLANEJADO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigoImo() { return codigoImo; }
    public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
    public String getPaisBandeira() { return paisBandeira; }
    public void setPaisBandeira(String paisBandeira) { this.paisBandeira = paisBandeira; }
    public String getEmpresaArmadora() { return empresaArmadora; }
    public void setEmpresaArmadora(String empresaArmadora) { this.empresaArmadora = empresaArmadora; }
    public String getTipoNavio() { return tipoNavio; }
    public void setTipoNavio(String tipoNavio) { this.tipoNavio = tipoNavio; }
    public BigDecimal getLoaMetros() { return loaMetros; }
    public void setLoaMetros(BigDecimal loaMetros) { this.loaMetros = loaMetros; }
    public BigDecimal getDwtToneladas() { return dwtToneladas; }
    public void setDwtToneladas(BigDecimal dwtToneladas) { this.dwtToneladas = dwtToneladas; }
    public Integer getQuantidadePoroes() { return quantidadePoroes; }
    public void setQuantidadePoroes(Integer quantidadePoroes) { this.quantidadePoroes = quantidadePoroes; }
    public StatusNavioSiderurgico getStatus() { return status; }
    public void setStatus(StatusNavioSiderurgico status) { this.status = status; }
}
