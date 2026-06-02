package br.com.cloudport.servicoyard.recursos.dto;

import br.com.cloudport.servicoyard.recursos.entidade.StatusReservaBerco;
import br.com.cloudport.servicoyard.recursos.entidade.TipoReservaBerco;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservaBercoDTO {

    private Long id;
    private String bercoCodigo;
    private String navioCodigo;
    private String navioNome;
    private LocalDateTime chegadaPrevista;
    private LocalDateTime saidaPrevista;
    private Integer comprimentoNavio;
    private BigDecimal caladoNavio;
    private Integer guinchesRequeridos;
    private String tipoCarga;
    private String zonaArmazenagem;
    private TipoReservaBerco tipoReserva;
    private StatusReservaBerco status;
    private Integer score;
    private String motivo;
    private LocalDateTime criadoEm;

    public ReservaBercoDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBercoCodigo() {
        return bercoCodigo;
    }

    public void setBercoCodigo(String bercoCodigo) {
        this.bercoCodigo = bercoCodigo;
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
}
