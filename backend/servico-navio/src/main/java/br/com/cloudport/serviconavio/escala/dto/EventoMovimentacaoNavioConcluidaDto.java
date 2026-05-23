package br.com.cloudport.serviconavio.escala.dto;

import java.time.OffsetDateTime;

public class EventoMovimentacaoNavioConcluidaDto {

    private Long idEscala;
    private Long idOrdemMovimentacao;
    private String codigoConteiner;
    private String tipoMovimentacao;
    private OffsetDateTime concluidoEm;
    private String statusEvento;

    public EventoMovimentacaoNavioConcluidaDto() {
    }

    public EventoMovimentacaoNavioConcluidaDto(Long idEscala,
                                               Long idOrdemMovimentacao,
                                               String codigoConteiner,
                                               String tipoMovimentacao,
                                               OffsetDateTime concluidoEm,
                                               String statusEvento) {
        this.idEscala = idEscala;
        this.idOrdemMovimentacao = idOrdemMovimentacao;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimentacao = tipoMovimentacao;
        this.concluidoEm = concluidoEm;
        this.statusEvento = statusEvento;
    }

    public Long getIdEscala() {
        return idEscala;
    }

    public void setIdEscala(Long idEscala) {
        this.idEscala = idEscala;
    }

    public Long getIdOrdemMovimentacao() {
        return idOrdemMovimentacao;
    }

    public void setIdOrdemMovimentacao(Long idOrdemMovimentacao) {
        this.idOrdemMovimentacao = idOrdemMovimentacao;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public String getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(String tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public OffsetDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(OffsetDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public String getStatusEvento() {
        return statusEvento;
    }

    public void setStatusEvento(String statusEvento) {
        this.statusEvento = statusEvento;
    }
}
