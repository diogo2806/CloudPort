package br.com.cloudport.servicoyard.recursos.dto;

public class EventoRecursosTempoRealDTO {

    private String tipoEvento;
    private ResumoRecursosDTO resumo;
    private RespostaAlocacaoBercoDTO alocacao;

    public EventoRecursosTempoRealDTO() {
    }

    public EventoRecursosTempoRealDTO(String tipoEvento, ResumoRecursosDTO resumo, RespostaAlocacaoBercoDTO alocacao) {
        this.tipoEvento = tipoEvento;
        this.resumo = resumo;
        this.alocacao = alocacao;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public ResumoRecursosDTO getResumo() {
        return resumo;
    }

    public void setResumo(ResumoRecursosDTO resumo) {
        this.resumo = resumo;
    }

    public RespostaAlocacaoBercoDTO getAlocacao() {
        return alocacao;
    }

    public void setAlocacao(RespostaAlocacaoBercoDTO alocacao) {
        this.alocacao = alocacao;
    }
}
