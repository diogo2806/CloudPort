package br.com.cloudport.serviconavio.escala.dto;

import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class LineUpPublicoDTO {

    private final String nomeNavio;
    private final String codigoImo;
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
    private final String berco;

    public LineUpPublicoDTO(String nomeNavio,
                            String codigoImo,
                            String viagemEntrada,
                            String viagemSaida,
                            FaseEscala fase,
                            LocalDateTime chegadaPrevista,
                            LocalDateTime atracacaoPrevista,
                            LocalDateTime partidaPrevista,
                            LocalDateTime chegadaEfetiva,
                            LocalDateTime atracacaoEfetiva,
                            LocalDateTime partidaEfetiva,
                            String berco) {
        this.nomeNavio = nomeNavio;
        this.codigoImo = codigoImo;
        this.viagemEntrada = viagemEntrada;
        this.viagemSaida = viagemSaida;
        this.fase = fase;
        this.chegadaPrevista = chegadaPrevista;
        this.atracacaoPrevista = atracacaoPrevista;
        this.partidaPrevista = partidaPrevista;
        this.chegadaEfetiva = chegadaEfetiva;
        this.atracacaoEfetiva = atracacaoEfetiva;
        this.partidaEfetiva = partidaEfetiva;
        this.berco = berco;
    }

    public String getNomeNavio() { return nomeNavio; }
    public String getCodigoImo() { return codigoImo; }
    public String getViagemEntrada() { return viagemEntrada; }
    public String getViagemSaida() { return viagemSaida; }
    public FaseEscala getFase() { return fase; }
    public LocalDateTime getChegadaPrevista() { return chegadaPrevista; }
    public LocalDateTime getAtracacaoPrevista() { return atracacaoPrevista; }
    public LocalDateTime getPartidaPrevista() { return partidaPrevista; }
    public LocalDateTime getChegadaEfetiva() { return chegadaEfetiva; }
    public LocalDateTime getAtracacaoEfetiva() { return atracacaoEfetiva; }
    public LocalDateTime getPartidaEfetiva() { return partidaEfetiva; }
    public String getBerco() { return berco; }
}
