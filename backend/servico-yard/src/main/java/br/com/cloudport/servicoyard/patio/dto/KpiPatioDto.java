package br.com.cloudport.servicoyard.patio.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class KpiPatioDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double yardDensity;
    private Double rehandleRatio;
    private Double equipmentUtilization;
    private Integer gateThroghput;
    private LocalDateTime atualizadoEm;

    // Status e cores
    private String statusYardDensity;
    private String statusRehandleRatio;
    private String statusEquipmentUtilization;
    private String statusGateThroghput;

    public KpiPatioDto() {
    }

    public KpiPatioDto(Double yardDensity, Double rehandleRatio,
                       Double equipmentUtilization, Integer gateThroghput,
                       LocalDateTime atualizadoEm) {
        this.yardDensity = yardDensity;
        this.rehandleRatio = rehandleRatio;
        this.equipmentUtilization = equipmentUtilization;
        this.gateThroghput = gateThroghput;
        this.atualizadoEm = atualizadoEm;
        calcularStatus();
    }

    private void calcularStatus() {
        this.statusYardDensity = determinarStatus(yardDensity, 60, 80);
        this.statusRehandleRatio = determinarStatus(rehandleRatio, 10, 20);
        this.statusEquipmentUtilization = determinarStatus(equipmentUtilization, 70, 85);
        this.statusGateThroghput = determinarStatus((double) gateThroghput, 8, 12);
    }

    private String determinarStatus(Double valor, double amarelo, double vermelho) {
        if (valor >= vermelho) {
            return "CRITICO";
        } else if (valor >= amarelo) {
            return "ATENCAO";
        } else {
            return "OK";
        }
    }

    // Getters e Setters
    public Double getYardDensity() {
        return yardDensity;
    }

    public void setYardDensity(Double yardDensity) {
        this.yardDensity = yardDensity;
    }

    public Double getRehandleRatio() {
        return rehandleRatio;
    }

    public void setRehandleRatio(Double rehandleRatio) {
        this.rehandleRatio = rehandleRatio;
    }

    public Double getEquipmentUtilization() {
        return equipmentUtilization;
    }

    public void setEquipmentUtilization(Double equipmentUtilization) {
        this.equipmentUtilization = equipmentUtilization;
    }

    public Integer getGateThroghput() {
        return gateThroghput;
    }

    public void setGateThroghput(Integer gateThroghput) {
        this.gateThroghput = gateThroghput;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getStatusYardDensity() {
        return statusYardDensity;
    }

    public void setStatusYardDensity(String statusYardDensity) {
        this.statusYardDensity = statusYardDensity;
    }

    public String getStatusRehandleRatio() {
        return statusRehandleRatio;
    }

    public void setStatusRehandleRatio(String statusRehandleRatio) {
        this.statusRehandleRatio = statusRehandleRatio;
    }

    public String getStatusEquipmentUtilization() {
        return statusEquipmentUtilization;
    }

    public void setStatusEquipmentUtilization(String statusEquipmentUtilization) {
        this.statusEquipmentUtilization = statusEquipmentUtilization;
    }

    public String getStatusGateThroghput() {
        return statusGateThroghput;
    }

    public void setStatusGateThroghput(String statusGateThroghput) {
        this.statusGateThroghput = statusGateThroghput;
    }
}
