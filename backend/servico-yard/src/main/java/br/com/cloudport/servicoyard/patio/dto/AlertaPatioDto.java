package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;

public class AlertaPatioDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TipoAlerta {
        REHANDLE,
        CONFLITO_INFRAESTRUTURA,
        GARGALO_EQUIPAMENTO
    }

    public enum NivelSeveridade {
        INFO,
        ATENCAO,
        CRITICO
    }

    private Long conteinerParioId;
    private String codigoConteiner;
    private TipoAlerta tipoAlerta;
    private NivelSeveridade nivelSeveridade;
    private String mensagem;
    private String recomendacao;

    public AlertaPatioDto() {
    }

    public AlertaPatioDto(Long conteinerParioId, String codigoConteiner, TipoAlerta tipoAlerta,
                          NivelSeveridade nivelSeveridade, String mensagem, String recomendacao) {
        this.conteinerParioId = conteinerParioId;
        this.codigoConteiner = codigoConteiner;
        this.tipoAlerta = tipoAlerta;
        this.nivelSeveridade = nivelSeveridade;
        this.mensagem = mensagem;
        this.recomendacao = recomendacao;
    }

    public Long getConteinerParioId() {
        return conteinerParioId;
    }

    public void setConteinerParioId(Long conteinerParioId) {
        this.conteinerParioId = conteinerParioId;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public TipoAlerta getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(TipoAlerta tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public NivelSeveridade getNivelSeveridade() {
        return nivelSeveridade;
    }

    public void setNivelSeveridade(NivelSeveridade nivelSeveridade) {
        this.nivelSeveridade = nivelSeveridade;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getRecomendacao() {
        return recomendacao;
    }

    public void setRecomendacao(String recomendacao) {
        this.recomendacao = recomendacao;
    }
}
