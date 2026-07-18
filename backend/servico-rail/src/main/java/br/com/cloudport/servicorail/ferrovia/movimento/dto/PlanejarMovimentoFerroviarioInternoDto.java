package br.com.cloudport.servicorail.ferrovia.movimento.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PlanejarMovimentoFerroviarioInternoDto {

    @NotNull(message = "A visita de trem deve ser informada.")
    private Long visitaTremId;

    @NotBlank(message = "A origem deve ser informada.")
    @Size(max = 120, message = "A origem deve ter no máximo 120 caracteres.")
    private String origem;

    @NotBlank(message = "O destino deve ser informado.")
    @Size(max = 120, message = "O destino deve ter no máximo 120 caracteres.")
    private String destino;

    @NotNull(message = "O início planejado deve ser informado.")
    private LocalDateTime inicioPlanejado;

    @NotNull(message = "O fim planejado deve ser informado.")
    private LocalDateTime fimPlanejado;

    @Size(max = 50, message = "O movimento aceita no máximo 50 rotas.")
    private List<@NotBlank @Size(max = 80) String> rotas = new ArrayList<>();

    @Size(max = 50, message = "O movimento aceita no máximo 50 linhas.")
    private List<@NotBlank @Size(max = 80) String> linhas = new ArrayList<>();

    @Size(max = 100, message = "O movimento aceita no máximo 100 trechos.")
    private List<@NotBlank @Size(max = 80) String> trechos = new ArrayList<>();

    @Size(max = 100, message = "O movimento aceita no máximo 100 switches.")
    private List<@NotBlank @Size(max = 80) String> switches = new ArrayList<>();

    public Long getVisitaTremId() {
        return visitaTremId;
    }

    public void setVisitaTremId(Long visitaTremId) {
        this.visitaTremId = visitaTremId;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public LocalDateTime getInicioPlanejado() {
        return inicioPlanejado;
    }

    public void setInicioPlanejado(LocalDateTime inicioPlanejado) {
        this.inicioPlanejado = inicioPlanejado;
    }

    public LocalDateTime getFimPlanejado() {
        return fimPlanejado;
    }

    public void setFimPlanejado(LocalDateTime fimPlanejado) {
        this.fimPlanejado = fimPlanejado;
    }

    public List<String> getRotas() {
        return rotas;
    }

    public void setRotas(List<String> rotas) {
        this.rotas = rotas;
    }

    public List<String> getLinhas() {
        return linhas;
    }

    public void setLinhas(List<String> linhas) {
        this.linhas = linhas;
    }

    public List<String> getTrechos() {
        return trechos;
    }

    public void setTrechos(List<String> trechos) {
        this.trechos = trechos;
    }

    public List<String> getSwitches() {
        return switches;
    }

    public void setSwitches(List<String> switches) {
        this.switches = switches;
    }
}
