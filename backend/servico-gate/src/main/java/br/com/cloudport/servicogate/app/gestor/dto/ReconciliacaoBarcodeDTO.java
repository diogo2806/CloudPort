package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.LocalDateTime;

public class ReconciliacaoBarcodeDTO {

    private Long id;
    private Long gatePassId;
    private String codigoGatePass;
    private TipoDesincroniaBarcode tipoDesinconia;
    private String descricao;
    private String barcodeEsperado;
    private String barcodeRecebido;
    private String statusTos;
    private String statusLocal;
    private Integer tempoPendenciaHoras;
    private LocalDateTime detectadoEm;
    private LocalDateTime resolvidoEm;
    private String resolucao;
    private boolean alertaEnviado;

    public static ReconciliacaoBarcodeDTO fromEntity(ReconciliacaoBarcode entity) {
        ReconciliacaoBarcodeDTO dto = new ReconciliacaoBarcodeDTO();
        dto.setId(entity.getId());
        dto.setGatePassId(entity.getGatePass().getId());
        dto.setCodigoGatePass(entity.getGatePass().getCodigo());
        dto.setTipoDesinconia(entity.getTipoDesinconia());
        dto.setDescricao(entity.getDescricao());
        dto.setBarcodeEsperado(entity.getBarcodeEsperado());
        dto.setBarcodeRecebido(entity.getBarcodeRecebido());
        dto.setStatusTos(entity.getStatusTos());
        dto.setStatusLocal(entity.getStatusLocal());
        dto.setTempoPendenciaHoras(entity.getTempoPendenciaHoras());
        dto.setDetectadoEm(entity.getDetectadoEm());
        dto.setResolvidoEm(entity.getResolvidoEm());
        dto.setResolucao(entity.getResolucao());
        dto.setAlertaEnviado(entity.isAlertaEnviado());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGatePassId() {
        return gatePassId;
    }

    public void setGatePassId(Long gatePassId) {
        this.gatePassId = gatePassId;
    }

    public String getCodigoGatePass() {
        return codigoGatePass;
    }

    public void setCodigoGatePass(String codigoGatePass) {
        this.codigoGatePass = codigoGatePass;
    }

    public TipoDesincroniaBarcode getTipoDesinconia() {
        return tipoDesinconia;
    }

    public void setTipoDesinconia(TipoDesincroniaBarcode tipoDesinconia) {
        this.tipoDesinconia = tipoDesinconia;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getBarcodeEsperado() {
        return barcodeEsperado;
    }

    public void setBarcodeEsperado(String barcodeEsperado) {
        this.barcodeEsperado = barcodeEsperado;
    }

    public String getBarcodeRecebido() {
        return barcodeRecebido;
    }

    public void setBarcodeRecebido(String barcodeRecebido) {
        this.barcodeRecebido = barcodeRecebido;
    }

    public String getStatusTos() {
        return statusTos;
    }

    public void setStatusTos(String statusTos) {
        this.statusTos = statusTos;
    }

    public String getStatusLocal() {
        return statusLocal;
    }

    public void setStatusLocal(String statusLocal) {
        this.statusLocal = statusLocal;
    }

    public Integer getTempoPendenciaHoras() {
        return tempoPendenciaHoras;
    }

    public void setTempoPendenciaHoras(Integer tempoPendenciaHoras) {
        this.tempoPendenciaHoras = tempoPendenciaHoras;
    }

    public LocalDateTime getDetectadoEm() {
        return detectadoEm;
    }

    public void setDetectadoEm(LocalDateTime detectadoEm) {
        this.detectadoEm = detectadoEm;
    }

    public LocalDateTime getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(LocalDateTime resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }

    public String getResolucao() {
        return resolucao;
    }

    public void setResolucao(String resolucao) {
        this.resolucao = resolucao;
    }

    public boolean isAlertaEnviado() {
        return alertaEnviado;
    }

    public void setAlertaEnviado(boolean alertaEnviado) {
        this.alertaEnviado = alertaEnviado;
    }
}
