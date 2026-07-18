package br.com.cloudport.servicoyard.scheduler.dto;

import java.math.BigDecimal;
import java.util.List;

public class SchedulerPositionCandidateDto {

    private Long id;
    private Integer linha;
    private Integer coluna;
    private String camada;
    private String bloco;
    private boolean ocupada;
    private String codigoOcupante;
    private boolean bloqueada;
    private boolean interditada;
    private boolean areaPermitida = true;
    private boolean reservadaPorOutro;
    private boolean allocationCompativel = true;
    private boolean reeferPermitida = true;
    private boolean imoPermitida = true;
    private boolean oogPermitida = true;
    private List<String> tiposCargaPermitidos = List.of();
    private BigDecimal pesoMaximoToneladas;
    private BigDecimal alturaMaximaMetros;
    private Integer capacidadePilha;
    private Long ocupacaoPilha;
    private Integer distanciaBerco;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public String getCamada() { return camada; }
    public void setCamada(String camada) { this.camada = camada; }
    public String getBloco() { return bloco; }
    public void setBloco(String bloco) { this.bloco = bloco; }
    public boolean isOcupada() { return ocupada; }
    public void setOcupada(boolean ocupada) { this.ocupada = ocupada; }
    public String getCodigoOcupante() { return codigoOcupante; }
    public void setCodigoOcupante(String codigoOcupante) { this.codigoOcupante = codigoOcupante; }
    public boolean isBloqueada() { return bloqueada; }
    public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
    public boolean isInterditada() { return interditada; }
    public void setInterditada(boolean interditada) { this.interditada = interditada; }
    public boolean isAreaPermitida() { return areaPermitida; }
    public void setAreaPermitida(boolean areaPermitida) { this.areaPermitida = areaPermitida; }
    public boolean isReservadaPorOutro() { return reservadaPorOutro; }
    public void setReservadaPorOutro(boolean reservadaPorOutro) { this.reservadaPorOutro = reservadaPorOutro; }
    public boolean isAllocationCompativel() { return allocationCompativel; }
    public void setAllocationCompativel(boolean allocationCompativel) { this.allocationCompativel = allocationCompativel; }
    public boolean isReeferPermitida() { return reeferPermitida; }
    public void setReeferPermitida(boolean reeferPermitida) { this.reeferPermitida = reeferPermitida; }
    public boolean isImoPermitida() { return imoPermitida; }
    public void setImoPermitida(boolean imoPermitida) { this.imoPermitida = imoPermitida; }
    public boolean isOogPermitida() { return oogPermitida; }
    public void setOogPermitida(boolean oogPermitida) { this.oogPermitida = oogPermitida; }
    public List<String> getTiposCargaPermitidos() { return tiposCargaPermitidos == null ? List.of() : tiposCargaPermitidos; }
    public void setTiposCargaPermitidos(List<String> tiposCargaPermitidos) { this.tiposCargaPermitidos = tiposCargaPermitidos; }
    public BigDecimal getPesoMaximoToneladas() { return pesoMaximoToneladas; }
    public void setPesoMaximoToneladas(BigDecimal pesoMaximoToneladas) { this.pesoMaximoToneladas = pesoMaximoToneladas; }
    public BigDecimal getAlturaMaximaMetros() { return alturaMaximaMetros; }
    public void setAlturaMaximaMetros(BigDecimal alturaMaximaMetros) { this.alturaMaximaMetros = alturaMaximaMetros; }
    public Integer getCapacidadePilha() { return capacidadePilha; }
    public void setCapacidadePilha(Integer capacidadePilha) { this.capacidadePilha = capacidadePilha; }
    public Long getOcupacaoPilha() { return ocupacaoPilha; }
    public void setOcupacaoPilha(Long ocupacaoPilha) { this.ocupacaoPilha = ocupacaoPilha; }
    public Integer getDistanciaBerco() { return distanciaBerco; }
    public void setDistanciaBerco(Integer distanciaBerco) { this.distanciaBerco = distanciaBerco; }
}
