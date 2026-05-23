package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class EscalaResumoDTO {

    private final Long id;
    private final Long navioId;
    private final String nomeNavio;
    private final String codigoImo;
    private final String viagemEntrada;
    private final FaseEscala fase;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime chegadaPrevista;
    private final String bercoPrevisto;

    public EscalaResumoDTO(Long id,
                           Long navioId,
                           String nomeNavio,
                           String codigoImo,
                           String viagemEntrada,
                           FaseEscala fase,
                           LocalDateTime chegadaPrevista,
                           String bercoPrevisto) {
        this.id = id;
        this.navioId = navioId;
        this.nomeNavio = nomeNavio;
        this.codigoImo = codigoImo;
        this.viagemEntrada = viagemEntrada;
        this.fase = fase;
        this.chegadaPrevista = chegadaPrevista;
        this.bercoPrevisto = bercoPrevisto;
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

    public String getViagemEntrada() {
        return viagemEntrada;
    }

    public FaseEscala getFase() {
        return fase;
    }

    public LocalDateTime getChegadaPrevista() {
        return chegadaPrevista;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }
}
