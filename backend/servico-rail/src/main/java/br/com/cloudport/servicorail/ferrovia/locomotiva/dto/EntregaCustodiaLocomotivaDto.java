package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class EntregaCustodiaLocomotivaDto {

    @NotBlank
    @Size(max = 120)
    private String nomeMaquinista;

    @NotBlank
    @Size(max = 80)
    private String documentoEntrega;

    @NotBlank
    @Size(max = 120)
    private String responsavelTerminal;

    private LocalDateTime entregueEm;

    @Size(max = 1000)
    private String observacoes;

    public String getNomeMaquinista() { return nomeMaquinista; }
    public void setNomeMaquinista(String nomeMaquinista) { this.nomeMaquinista = nomeMaquinista; }
    public String getDocumentoEntrega() { return documentoEntrega; }
    public void setDocumentoEntrega(String documentoEntrega) { this.documentoEntrega = documentoEntrega; }
    public String getResponsavelTerminal() { return responsavelTerminal; }
    public void setResponsavelTerminal(String responsavelTerminal) { this.responsavelTerminal = responsavelTerminal; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime entregueEm) { this.entregueEm = entregueEm; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
