package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.TipoGeometriaPatio;
import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GeometriaPatioRequisicaoDto {

    @NotBlank
    @Size(max = 80)
    private String codigo;

    @NotNull
    private TipoGeometriaPatio tipo;

    @Size(max = 40)
    private String bloco;

    private Integer linha;
    private Integer coluna;

    @NotNull
    private JsonNode geoJson;

    @NotBlank
    @Size(max = 500)
    private String motivo;

    @Size(max = 120)
    private String usuario;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoGeometriaPatio getTipo() {
        return tipo;
    }

    public void setTipo(TipoGeometriaPatio tipo) {
        this.tipo = tipo;
    }

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public JsonNode getGeoJson() {
        return geoJson;
    }

    public void setGeoJson(JsonNode geoJson) {
        this.geoJson = geoJson;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
