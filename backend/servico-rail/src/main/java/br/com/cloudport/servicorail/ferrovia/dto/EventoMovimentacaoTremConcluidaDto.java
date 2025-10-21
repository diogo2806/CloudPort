package br.com.cloudport.servicorail.ferrovia.dto;

import java.time.OffsetDateTime;

public class EventoMovimentacaoTremConcluidaDto {

    private Long idVisitaTrem;
    private Long idOrdemMovimentacao;
    private String codigoConteiner;
    private String tipoMovimentacao;
    private OffsetDateTime concluidoEm;
    private String statusEvento;

    public EventoMovimentacaoTremConcluidaDto() {
    }

    public EventoMovimentacaoTremConcluidaDto(Long idVisitaTrem,
                                              Long idOrdemMovimentacao,
                                              String codigoConteiner,
                                              String tipoMovimentacao,
                                              OffsetDateTime concluidoEm,
                                              String statusEvento) {
        this.idVisitaTrem = idVisitaTrem;
        this.idOrdemMovimentacao = idOrdemMovimentacao;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimentacao = tipoMovimentacao;
        this.concluidoEm = concluidoEm;
        this.statusEvento = statusEvento;
    }

    public Long getIdVisitaTrem() {
        return idVisitaTrem;
    }

    public void setIdVisitaTrem(Long idVisitaTrem) {
        this.idVisitaTrem = idVisitaTrem;
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
