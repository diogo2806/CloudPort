package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ConfirmacaoEmbarqueLocomotivaDto {

    private LocalDateTime embarcadaEm;

    @NotBlank
    @Size(max = 120)
    private String posicaoReal;

    @Size(max = 1000)
    private String observacoes;

    public LocalDateTime getEmbarcadaEm() { return embarcadaEm; }
    public void setEmbarcadaEm(LocalDateTime embarcadaEm) { this.embarcadaEm = embarcadaEm; }
    public String getPosicaoReal() { return posicaoReal; }
    public void setPosicaoReal(String posicaoReal) { this.posicaoReal = posicaoReal; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
