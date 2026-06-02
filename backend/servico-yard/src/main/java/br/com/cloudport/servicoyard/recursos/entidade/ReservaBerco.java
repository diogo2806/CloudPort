package br.com.cloudport.servicoyard.recursos.entidade;

import java.math.BigDecimal;
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
import javax.persistence.Table;

@Entity
@Table(name = "reserva_berco")
public class ReservaBerco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "berco_id", nullable = false)
    private BercoPortuario berco;

    @Column(name = "navio_codigo", nullable = false, length = 50)
    private String navioCodigo;

    @Column(name = "navio_nome", nullable = false, length = 120)
    private String navioNome;

    @Column(name = "chegada_prevista", nullable = false)
    private LocalDateTime chegadaPrevista;

    @Column(name = "saida_prevista", nullable = false)
    private LocalDateTime saidaPrevista;

    @Column(name = "comprimento_navio", nullable = false)
    private Integer comprimentoNavio;

    @Column(name = "calado_navio", nullable = false, precision = 6, scale = 2)
    private BigDecimal caladoNavio;

    @Column(name = "guinches_requeridos", nullable = false)
    private Integer guinchesRequeridos;

    @Column(name = "tipo_carga", nullable = false, length = 40)
    private String tipoCarga;

    @Column(name = "zona_armazenagem", nullable = false, length = 40)
    private String zonaArmazenagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reserva", nullable = false, length = 30)
    private TipoReservaBerco tipoReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusReservaBerco status;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "motivo", nullable = false, length = 250)
    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BercoPortuario getBerco() {
        return berco;
    }

    public void setBerco(BercoPortuario berco) {
        this.berco = berco;
    }

    public String getNavioCodigo() {
        return navioCodigo;
    }

    public void setNavioCodigo(String navioCodigo) {
        this.navioCodigo = navioCodigo;
    }

    public String getNavioNome() {
        return navioNome;
    }

    public void setNavioNome(String navioNome) {
        this.navioNome = navioNome;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public void setChegadaPrevista(LocalDateTime chegadaPrevista) {
        this.chegadaPrevista = chegadaPrevista;
    }

    public LocalDateTime getSaidaPrevista() {
        return saidaPrevista;
    }

    public void setSaidaPrevista(LocalDateTime saidaPrevista) {
        this.saidaPrevista = saidaPrevista;
    }

    public Integer getComprimentoNavio() {
        return comprimentoNavio;
    }

    public void setComprimentoNavio(Integer comprimentoNavio) {
        this.comprimentoNavio = comprimentoNavio;
    }

    public BigDecimal getCaladoNavio() {
        return caladoNavio;
    }

    public void setCaladoNavio(BigDecimal caladoNavio) {
        this.caladoNavio = caladoNavio;
    }

    public Integer getGuinchesRequeridos() {
        return guinchesRequeridos;
    }

    public void setGuinchesRequeridos(Integer guinchesRequeridos) {
        this.guinchesRequeridos = guinchesRequeridos;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getZonaArmazenagem() {
        return zonaArmazenagem;
    }

    public void setZonaArmazenagem(String zonaArmazenagem) {
        this.zonaArmazenagem = zonaArmazenagem;
    }

    public TipoReservaBerco getTipoReserva() {
        return tipoReserva;
    }

    public void setTipoReserva(TipoReservaBerco tipoReserva) {
        this.tipoReserva = tipoReserva;
    }

    public StatusReservaBerco getStatus() {
        return status;
    }

    public void setStatus(StatusReservaBerco status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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
