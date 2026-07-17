package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LinhaUpEscalaDTO {

    private final Long id;
    private final Long navioId;
    private final String nomeNavio;
    private final String codigoImo;
    private final String empresaArmadora;
    private final Integer capacidadeTeu;
    private final BigDecimal loaMetros;
    private final String viagemEntrada;
    private final String viagemSaida;
    private final FaseEscala fase;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime chegadaPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime atracacaoPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime partidaPrevista;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime chegadaEfetiva;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime atracacaoEfetiva;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime partidaEfetiva;
    private final String bercoPrevisto;
    private final String bercoAtual;
    private final String observacoes;

    public LinhaUpEscalaDTO(Long id,
                            Long navioId,
                            String nomeNavio,
                            String codigoImo,
                            String empresaArmadora,
                            Integer capacidadeTeu,
                            BigDecimal loaMetros,
                            String viagemEntrada,
                            String viagemSaida,
                            FaseEscala fase,
                            LocalDateTime chegadaPrevista,
                            LocalDateTime atracacaoPrevista,
                            LocalDateTime partidaPrevista,
                            LocalDateTime chegadaEfetiva,
                            LocalDateTime atracacaoEfetiva,
                            LocalDateTime partidaEfetiva,
                            String bercoPrevisto,
                            String bercoAtual,
                            String observacoes) {
        this.id = id;
        this.navioId = navioId;
        this.nomeNavio = nomeNavio;
        this.codigoImo = codigoImo;
        this.empresaArmadora = empresaArmadora;
        this.capacidadeTeu = capacidadeTeu;
        this.loaMetros = loaMetros;
        this.viagemEntrada = viagemEntrada;
        this.viagemSaida = viagemSaida;
        this.fase = fase;
        this.chegadaPrevista = chegadaPrevista;
        this.atracacaoPrevista = atracacaoPrevista;
        this.partidaPrevista = partidaPrevista;
        this.chegadaEfetiva = chegadaEfetiva;
        this.atracacaoEfetiva = atracacaoEfetiva;
        this.partidaEfetiva = partidaEfetiva;
        this.bercoPrevisto = bercoPrevisto;
        this.bercoAtual = bercoAtual;
        this.observacoes = observacoes;
    }

    public static LinhaUpEscalaDTO deEntidade(Escala escala) {
        Navio navio = escala.getNavio();
        return new LinhaUpEscalaDTO(
                escala.getId(),
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getEmpresaArmadora(),
                navio.getCapacidadeTeu(),
                navio.getLoaMetros(),
                escala.getViagemEntrada(),
                escala.getViagemSaida(),
                escala.getFase(),
                escala.getChegadaPrevista(),
                escala.getAtracacaoPrevista(),
                escala.getPartidaPrevista(),
                escala.getChegadaEfetiva(),
                escala.getAtracacaoEfetiva(),
                escala.getPartidaEfetiva(),
                escala.getBercoPrevisto(),
                escala.getBercoAtual(),
                escala.getObservacoes()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getNavioId() {
        return navioId;
    }

    public String getNomeNavio() {
        return nomeNavio;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public String getEmpresaArmadora() {
        return empresaArmadora;
    }

    public Integer getCapacidadeTeu() {
        return capacidadeTeu;
    }

    public BigDecimal getLoaMetros() {
        return loaMetros;
    }

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public String getViagemSaida() {
        return viagemSaida;
    }

    public FaseEscala getFase() {
        return fase;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public LocalDateTime getPartidaPrevista() {
        return partidaPrevista;
    }

    public LocalDateTime getChegadaEfetiva() {
        return chegadaEfetiva;
    }

    public LocalDateTime getAtracacaoEfetiva() {
        return atracacaoEfetiva;
    }

    public LocalDateTime getPartidaEfetiva() {
        return partidaEfetiva;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
