package br.com.cloudport.servicoyard.container.dto;

import java.time.OffsetDateTime;

public class MovimentacaoNavioConcluidaEventoDto {

    private Long idEscala;
    private Long idOrdemMovimentacao;
    private String codigoConteiner;
    private String tipoMovimentacao;
    private OffsetDateTime concluidoEm;
    private String statusEvento;

    public MovimentacaoNavioConcluidaEventoDto() {
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
