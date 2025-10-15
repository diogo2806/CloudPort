package br.com.cloudport.servicogate.app.transparencia.dto;

import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.time.LocalDateTime;

public class DashboardFiltroDTO {

    private LocalDateTime inicio;
    private LocalDateTime fim;
    private Long transportadoraId;
    private TipoOperacao tipoOperacao;

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public Long getTransportadoraId() {
        return transportadoraId;
    }

    public void setTransportadoraId(Long transportadoraId) {
        this.transportadoraId = transportadoraId;
    }

    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacao tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }
}
