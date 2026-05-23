package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusVisitaNavio;
import java.time.LocalDateTime;

public class VisitaNavioResumoDTO {

    private final Long identificador;
    private final String navioNome;
    private final String codigoImo;
    private final String numeroViagem;
    private final String bercoNome;
    private final LocalDateTime atracacaoPrevista;
    private final LocalDateTime desatracacaoPrevista;
    private final StatusVisitaNavio status;
    private final String servicoCodigo;

    public VisitaNavioResumoDTO(Long identificador, String navioNome, String codigoImo, String numeroViagem,
                                String bercoNome, LocalDateTime atracacaoPrevista,
                                LocalDateTime desatracacaoPrevista, StatusVisitaNavio status,
                                String servicoCodigo) {
        this.identificador = identificador;
        this.navioNome = navioNome;
        this.codigoImo = codigoImo;
        this.numeroViagem = numeroViagem;
        this.bercoNome = bercoNome;
        this.atracacaoPrevista = atracacaoPrevista;
        this.desatracacaoPrevista = desatracacaoPrevista;
        this.status = status;
        this.servicoCodigo = servicoCodigo;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getNavioNome() {
        return navioNome;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public String getNumeroViagem() {
        return numeroViagem;
    }

    public String getBercoNome() {
        return bercoNome;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public LocalDateTime getDesatracacaoPrevista() {
        return desatracacaoPrevista;
    }

    public StatusVisitaNavio getStatus() {
        return status;
    }

    public String getServicoCodigo() {
        return servicoCodigo;
    }
}
