package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import java.util.List;

public class FilaOrdemTrabalhoPatioDto {

    private String identificador;
    private String agrupamento;
    private Long visitaNavioId;
    private String berco;
    private String blocoZona;
    private Integer sequenciaInicial;
    private StatusOrdemTrabalhoPatio status;
    private long totalOrdens;
    private List<OrdemTrabalhoPatioRespostaDto> ordens;

    public FilaOrdemTrabalhoPatioDto() {
    }

    public FilaOrdemTrabalhoPatioDto(String identificador,
                                     String agrupamento,
                                     Long visitaNavioId,
                                     String berco,
                                     String blocoZona,
                                     Integer sequenciaInicial,
                                     StatusOrdemTrabalhoPatio status,
                                     long totalOrdens,
                                     List<OrdemTrabalhoPatioRespostaDto> ordens) {
        this.identificador = identificador;
        this.agrupamento = agrupamento;
        this.visitaNavioId = visitaNavioId;
        this.berco = berco;
        this.blocoZona = blocoZona;
        this.sequenciaInicial = sequenciaInicial;
        this.status = status;
        this.totalOrdens = totalOrdens;
        this.ordens = ordens;
    }

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getAgrupamento() { return agrupamento; }
    public void setAgrupamento(String agrupamento) { this.agrupamento = agrupamento; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public String getBlocoZona() { return blocoZona; }
    public void setBlocoZona(String blocoZona) { this.blocoZona = blocoZona; }
    public Integer getSequenciaInicial() { return sequenciaInicial; }
    public void setSequenciaInicial(Integer sequenciaInicial) { this.sequenciaInicial = sequenciaInicial; }
    public StatusOrdemTrabalhoPatio getStatus() { return status; }
    public void setStatus(StatusOrdemTrabalhoPatio status) { this.status = status; }
    public long getTotalOrdens() { return totalOrdens; }
    public void setTotalOrdens(long totalOrdens) { this.totalOrdens = totalOrdens; }
    public List<OrdemTrabalhoPatioRespostaDto> getOrdens() { return ordens; }
    public void setOrdens(List<OrdemTrabalhoPatioRespostaDto> ordens) { this.ordens = ordens; }
}
