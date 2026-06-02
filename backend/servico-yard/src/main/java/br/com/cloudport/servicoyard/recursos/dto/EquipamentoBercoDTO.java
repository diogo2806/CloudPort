package br.com.cloudport.servicoyard.recursos.dto;

import br.com.cloudport.servicoyard.recursos.entidade.StatusEquipamentoBerco;
import java.time.LocalDateTime;

public class EquipamentoBercoDTO {

    private String identificador;
    private String tipo;
    private String bercoCodigo;
    private StatusEquipamentoBerco status;
    private LocalDateTime ultimaVerificacao;

    public EquipamentoBercoDTO() {
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getBercoCodigo() {
        return bercoCodigo;
    }

    public void setBercoCodigo(String bercoCodigo) {
        this.bercoCodigo = bercoCodigo;
    }

    public StatusEquipamentoBerco getStatus() {
        return status;
    }

    public void setStatus(StatusEquipamentoBerco status) {
        this.status = status;
    }

    public LocalDateTime getUltimaVerificacao() {
        return ultimaVerificacao;
    }

    public void setUltimaVerificacao(LocalDateTime ultimaVerificacao) {
        this.ultimaVerificacao = ultimaVerificacao;
    }
}
