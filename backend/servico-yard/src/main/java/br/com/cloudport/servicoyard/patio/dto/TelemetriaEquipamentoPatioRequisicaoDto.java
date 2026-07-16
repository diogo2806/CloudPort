package br.com.cloudport.servicoyard.patio.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public class TelemetriaEquipamentoPatioRequisicaoDto {

    private Double latitude;
    private Double longitude;
    private Double coordenadaX;
    private Double coordenadaY;
    private Double heading;
    private Integer linha;
    private Integer coluna;
    private String posicaoMaisProxima;

    @PositiveOrZero
    private Integer distanciaPosicaoCentimetros;

    private Boolean dentroDaPosicao;

    @NotBlank
    private String origem;

    private String operadorVmt;
    private String statusVmt;
    private Long workInstructionAtualId;

    @NotNull
    @PositiveOrZero
    private Long sequencia;

    @NotNull
    private LocalDateTime capturadoEm;

    @AssertTrue(message = "Informe latitude/longitude ou coordenadas X/Y da telemetria.")
    public boolean isCoordenadaValida() {
        boolean geodesica = latitude != null && longitude != null;
        boolean cartesiana = coordenadaX != null && coordenadaY != null;
        return geodesica || cartesiana;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getCoordenadaX() { return coordenadaX; }
    public void setCoordenadaX(Double coordenadaX) { this.coordenadaX = coordenadaX; }
    public Double getCoordenadaY() { return coordenadaY; }
    public void setCoordenadaY(Double coordenadaY) { this.coordenadaY = coordenadaY; }
    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public String getPosicaoMaisProxima() { return posicaoMaisProxima; }
    public void setPosicaoMaisProxima(String posicaoMaisProxima) { this.posicaoMaisProxima = posicaoMaisProxima; }
    public Integer getDistanciaPosicaoCentimetros() { return distanciaPosicaoCentimetros; }
    public void setDistanciaPosicaoCentimetros(Integer distanciaPosicaoCentimetros) { this.distanciaPosicaoCentimetros = distanciaPosicaoCentimetros; }
    public Boolean getDentroDaPosicao() { return dentroDaPosicao; }
    public void setDentroDaPosicao(Boolean dentroDaPosicao) { this.dentroDaPosicao = dentroDaPosicao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getOperadorVmt() { return operadorVmt; }
    public void setOperadorVmt(String operadorVmt) { this.operadorVmt = operadorVmt; }
    public String getStatusVmt() { return statusVmt; }
    public void setStatusVmt(String statusVmt) { this.statusVmt = statusVmt; }
    public Long getWorkInstructionAtualId() { return workInstructionAtualId; }
    public void setWorkInstructionAtualId(Long workInstructionAtualId) { this.workInstructionAtualId = workInstructionAtualId; }
    public Long getSequencia() { return sequencia; }
    public void setSequencia(Long sequencia) { this.sequencia = sequencia; }
    public LocalDateTime getCapturadoEm() { return capturadoEm; }
    public void setCapturadoEm(LocalDateTime capturadoEm) { this.capturadoEm = capturadoEm; }
}
