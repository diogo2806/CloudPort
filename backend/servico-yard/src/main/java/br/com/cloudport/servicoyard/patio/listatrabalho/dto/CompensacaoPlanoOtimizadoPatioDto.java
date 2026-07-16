package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto.EstadoAnteriorOrdemDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto.EstadoAnteriorWorkQueueDto;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class CompensacaoPlanoOtimizadoPatioDto {

    @NotBlank
    private String planoId;

    @NotNull
    private Long visitaNavioId;

    @NotBlank
    private String usuario;

    @NotBlank
    private String motivo;

    @Valid
    @NotEmpty
    private List<EstadoAnteriorOrdemDto> estadosAnteriores = new ArrayList<>();

    @Valid
    private List<EstadoAnteriorWorkQueueDto> estadosAnterioresWorkQueues = new ArrayList<>();

    public String getPlanoId() { return planoId; }
    public void setPlanoId(String planoId) { this.planoId = planoId; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public List<EstadoAnteriorOrdemDto> getEstadosAnteriores() { return estadosAnteriores; }
    public void setEstadosAnteriores(List<EstadoAnteriorOrdemDto> estadosAnteriores) { this.estadosAnteriores = estadosAnteriores; }
    public List<EstadoAnteriorWorkQueueDto> getEstadosAnterioresWorkQueues() { return estadosAnterioresWorkQueues; }
    public void setEstadosAnterioresWorkQueues(List<EstadoAnteriorWorkQueueDto> estadosAnterioresWorkQueues) {
        this.estadosAnterioresWorkQueues = estadosAnterioresWorkQueues;
    }
}
