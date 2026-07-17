package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.ModalidadeEmbarqueLocomotiva;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class PlanejamentoEmbarqueLocomotivaDto {

    @NotNull
    @Positive
    private Long visitaNavioId;

    @NotBlank
    @Size(max = 60)
    private String codigoVisitaNavio;

    @NotNull
    private ModalidadeEmbarqueLocomotiva modalidadeEmbarque;

    @NotBlank
    @Size(max = 80)
    private String deckPlanejado;

    @NotBlank
    @Size(max = 120)
    private String posicaoPlanejada;

    @Size(max = 1000)
    private String observacoes;

    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getCodigoVisitaNavio() { return codigoVisitaNavio; }
    public void setCodigoVisitaNavio(String codigoVisitaNavio) { this.codigoVisitaNavio = codigoVisitaNavio; }
    public ModalidadeEmbarqueLocomotiva getModalidadeEmbarque() { return modalidadeEmbarque; }
    public void setModalidadeEmbarque(ModalidadeEmbarqueLocomotiva modalidadeEmbarque) { this.modalidadeEmbarque = modalidadeEmbarque; }
    public String getDeckPlanejado() { return deckPlanejado; }
    public void setDeckPlanejado(String deckPlanejado) { this.deckPlanejado = deckPlanejado; }
    public String getPosicaoPlanejada() { return posicaoPlanejada; }
    public void setPosicaoPlanejada(String posicaoPlanejada) { this.posicaoPlanejada = posicaoPlanejada; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
