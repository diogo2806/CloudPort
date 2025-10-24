package br.com.cloudport.servicoyard.patio.enumeracao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum StatusServicoPatioEnum {
    DISPONIVEL("Disponível"),
    INDISPONIVEL("Indisponível");

    private static final Map<String, StatusServicoPatioEnum> NOME_PARA_ENUM_MAP;
    private static final Map<String, StatusServicoPatioEnum> DESCRICAO_PARA_ENUM_MAP;

    static {
        Map<String, StatusServicoPatioEnum> mapaNome = new HashMap<>();
        Map<String, StatusServicoPatioEnum> mapaDescricao = new HashMap<>();
        for (StatusServicoPatioEnum status : values()) {
            mapaNome.put(status.name().toLowerCase(Locale.ROOT), status);
            mapaDescricao.put(status.descricao.toLowerCase(Locale.ROOT), status);
        }
        NOME_PARA_ENUM_MAP = Collections.unmodifiableMap(mapaNome);
        DESCRICAO_PARA_ENUM_MAP = Collections.unmodifiableMap(mapaDescricao);
    }

    private final String descricao;

    StatusServicoPatioEnum(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static StatusServicoPatioEnum fromString(String valor) {
        if (valor == null) {
            return null;
        }
        String chaveNormalizada = valor.trim().toLowerCase(Locale.ROOT);
        StatusServicoPatioEnum porNome = NOME_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porNome != null) {
            return porNome;
        }
        StatusServicoPatioEnum porDescricao = DESCRICAO_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porDescricao != null) {
            return porDescricao;
        }
        throw new IllegalArgumentException("Valor inválido para StatusServicoPatioEnum: '" + valor + "'");
    }
}
