package br.com.cloudport.serviconavio.estiva.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;

public class EmbarqueDiretoGateDTO {

    @NotBlank
    private String codigoConteiner;

    private LocalDateTime embarcadoEm;

    public String getCodigoConteiner() { return codigoConteiner; }
    public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
    public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
    public void setEmbarcadoEm(LocalDateTime embarcadoEm) { this.embarcadoEm = embarcadoEm; }
}
