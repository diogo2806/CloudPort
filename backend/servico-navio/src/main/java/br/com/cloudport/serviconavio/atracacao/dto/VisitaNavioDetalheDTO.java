package br.com.cloudport.serviconavio.atracacao.dto;

import br.com.cloudport.serviconavio.atracacao.entidade.StatusVisitaNavio;
import java.time.LocalDateTime;
import java.util.List;

public class VisitaNavioDetalheDTO {

    private final Long identificador;
    private final Long navioId;
    private final String navioNome;
    private final String codigoImo;
    private final String numeroViagem;
    private final Long bercoId;
    private final String bercoNome;
    private final LocalDateTime atracacaoPrevista;
    private final LocalDateTime atracacaoEfetiva;
    private final LocalDateTime desatracacaoPrevista;
    private final LocalDateTime desatracacaoEfetiva;
    private final StatusVisitaNavio status;
    private final String observacoes;
    private final List<OperacaoNavioConteinerDTO> operacoes;

    public VisitaNavioDetalheDTO(Long identificador, Long navioId, String navioNome, String codigoImo,
                                 String numeroViagem, Long bercoId, String bercoNome,
                                 LocalDateTime atracacaoPrevista, LocalDateTime atracacaoEfetiva,
                                 LocalDateTime desatracacaoPrevista, LocalDateTime desatracacaoEfetiva,
                                 StatusVisitaNavio status, String observacoes,
                                 List<OperacaoNavioConteinerDTO> operacoes) {
        this.identificador = identificador;
        this.navioId = navioId;
        this.navioNome = navioNome;
        this.codigoImo = codigoImo;
        this.numeroViagem = numeroViagem;
        this.bercoId = bercoId;
        this.bercoNome = bercoNome;
        this.atracacaoPrevista = atracacaoPrevista;
        this.atracacaoEfetiva = atracacaoEfetiva;
        this.desatracacaoPrevista = desatracacaoPrevista;
        this.desatracacaoEfetiva = desatracacaoEfetiva;
        this.status = status;
        this.observacoes = observacoes;
        this.operacoes = operacoes;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public Long getNavioId() {
        return navioId;
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

    public Long getBercoId() {
        return bercoId;
    }

    public String getBercoNome() {
        return bercoNome;
    }

    public LocalDateTime getAtracacaoPrevista() {
        return atracacaoPrevista;
    }

    public LocalDateTime getAtracacaoEfetiva() {
        return atracacaoEfetiva;
    }

    public LocalDateTime getDesatracacaoPrevista() {
        return desatracacaoPrevista;
    }

    public LocalDateTime getDesatracacaoEfetiva() {
        return desatracacaoEfetiva;
    }

    public StatusVisitaNavio getStatus() {
        return status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<OperacaoNavioConteinerDTO> getOperacoes() {
        return operacoes;
    }
}
