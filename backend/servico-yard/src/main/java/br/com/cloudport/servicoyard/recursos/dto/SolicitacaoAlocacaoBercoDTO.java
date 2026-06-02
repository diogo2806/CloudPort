package br.com.cloudport.servicoyard.recursos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SolicitacaoAlocacaoBercoDTO {

    @NotBlank
    private String navioCodigo;

    @NotBlank
    private String navioNome;

    @NotNull
    private LocalDateTime chegadaPrevista;

    @NotNull
    private LocalDateTime saidaPrevista;

    @NotNull
    @Min(1)
    private Integer comprimentoNavio;

    @NotNull
    private BigDecimal caladoNavio;

    @NotNull
    @Min(1)
    private Integer guinchesRequeridos;

    @NotBlank
    private String tipoCarga;

    @NotBlank
    private String zonaArmazenagem;

    @Min(0)
    private Integer toneladasPrevistas;

    private String bercoPreferido;
    private boolean confirmar;

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

    public Integer getToneladasPrevistas() {
        return toneladasPrevistas;
    }

    public void setToneladasPrevistas(Integer toneladasPrevistas) {
        this.toneladasPrevistas = toneladasPrevistas;
    }

    public String getBercoPreferido() {
        return bercoPreferido;
    }

    public void setBercoPreferido(String bercoPreferido) {
        this.bercoPreferido = bercoPreferido;
    }

    public boolean isConfirmar() {
        return confirmar;
    }

    public void setConfirmar(boolean confirmar) {
        this.confirmar = confirmar;
    }
}
