package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.TipoGeometriaPatio;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public class GeometriaPatioDto {

    private final Long id;
    private final String codigo;
    private final TipoGeometriaPatio tipo;
    private final String bloco;
    private final Integer linha;
    private final Integer coluna;
    private final JsonNode geoJson;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final String criadoPor;
    private final String atualizadoPor;
    private final String motivoAtualizacao;

    public GeometriaPatioDto(Long id,
                             String codigo,
                             TipoGeometriaPatio tipo,
                             String bloco,
                             Integer linha,
                             Integer coluna,
                             JsonNode geoJson,
                             LocalDateTime criadoEm,
                             LocalDateTime atualizadoEm,
                             String criadoPor,
                             String atualizadoPor,
                             String motivoAtualizacao) {
        this.id = id;
        this.codigo = codigo;
        this.tipo = tipo;
        this.bloco = bloco;
        this.linha = linha;
        this.coluna = coluna;
        this.geoJson = geoJson;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.criadoPor = criadoPor;
        this.atualizadoPor = atualizadoPor;
        this.motivoAtualizacao = motivoAtualizacao;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoGeometriaPatio getTipo() {
        return tipo;
    }

    public String getBloco() {
        return bloco;
    }

    public Integer getLinha() {
        return linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public JsonNode getGeoJson() {
        return geoJson;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }

    public String getMotivoAtualizacao() {
        return motivoAtualizacao;
    }
}
