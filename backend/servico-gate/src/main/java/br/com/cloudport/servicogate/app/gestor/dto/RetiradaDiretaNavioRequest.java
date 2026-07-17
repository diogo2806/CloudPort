package br.com.cloudport.servicogate.app.gestor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class RetiradaDiretaNavioRequest {

    @NotBlank
    @Size(max = 80)
    private String codigoAutorizacao;

    @NotBlank
    @Size(max = 80)
    private String identificadorCarga;

    @Size(max = 60)
    private String tipoCarga;

    @NotBlank
    @Size(max = 80)
    private String visitaNavio;

    @NotBlank
    @Size(max = 120)
    private String clienteNome;

    @NotBlank
    @Size(max = 30)
    private String clienteDocumento;

    @NotNull
    private Boolean documentosValidados;

    @NotNull
    private Boolean liberacaoAduaneiraConfirmada;

    @NotNull
    private Boolean cargaDescarregada;

    @NotNull
    private Boolean condutorHabilitado;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Size(max = 500)
    private String observacao;

    public String getCodigoAutorizacao() {
        return codigoAutorizacao;
    }

    public void setCodigoAutorizacao(String codigoAutorizacao) {
        this.codigoAutorizacao = codigoAutorizacao;
    }

    public String getIdentificadorCarga() {
        return identificadorCarga;
    }

    public void setIdentificadorCarga(String identificadorCarga) {
        this.identificadorCarga = identificadorCarga;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getVisitaNavio() {
        return visitaNavio;
    }

    public void setVisitaNavio(String visitaNavio) {
        this.visitaNavio = visitaNavio;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }

    public Boolean getDocumentosValidados() {
        return documentosValidados;
    }

    public void setDocumentosValidados(Boolean documentosValidados) {
        this.documentosValidados = documentosValidados;
    }

    public Boolean getLiberacaoAduaneiraConfirmada() {
        return liberacaoAduaneiraConfirmada;
    }

    public void setLiberacaoAduaneiraConfirmada(Boolean liberacaoAduaneiraConfirmada) {
        this.liberacaoAduaneiraConfirmada = liberacaoAduaneiraConfirmada;
    }

    public Boolean getCargaDescarregada() {
        return cargaDescarregada;
    }

    public void setCargaDescarregada(Boolean cargaDescarregada) {
        this.cargaDescarregada = cargaDescarregada;
    }

    public Boolean getCondutorHabilitado() {
        return condutorHabilitado;
    }

    public void setCondutorHabilitado(Boolean condutorHabilitado) {
        this.condutorHabilitado = condutorHabilitado;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
