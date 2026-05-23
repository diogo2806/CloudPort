package br.com.cloudport.servicoyard.container.enumeracao;

import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum TipoMovimentacaoNavioEnum {
    DESCARGA_NAVIO(
            "Descarga do navio",
            TipoOperacaoConteiner.DESCARGA_NAVIO,
            "Descarga do navio concluída e contêiner disponível no pátio.",
            StatusOperacionalConteiner.ALOCADO
    ),
    CARGA_NAVIO(
            "Carga no navio",
            TipoOperacaoConteiner.CARGA_NAVIO,
            "Carga no navio concluída e contêiner embarcado.",
            StatusOperacionalConteiner.EM_TRANSFERENCIA
    );

    private static final Map<String, TipoMovimentacaoNavioEnum> NOME_PARA_ENUM_MAP;
    private static final Map<String, TipoMovimentacaoNavioEnum> DESCRICAO_PARA_ENUM_MAP;

    static {
        Map<String, TipoMovimentacaoNavioEnum> mapaNome = new HashMap<>();
        Map<String, TipoMovimentacaoNavioEnum> mapaDescricao = new HashMap<>();
        for (TipoMovimentacaoNavioEnum tipo : values()) {
            mapaNome.put(tipo.name().toLowerCase(Locale.ROOT), tipo);
            mapaDescricao.put(tipo.descricao.toLowerCase(Locale.ROOT), tipo);
        }
        NOME_PARA_ENUM_MAP = Collections.unmodifiableMap(mapaNome);
        DESCRICAO_PARA_ENUM_MAP = Collections.unmodifiableMap(mapaDescricao);
    }

    private final String descricao;
    private final TipoOperacaoConteiner tipoOperacaoConteiner;
    private final String descricaoHistorico;
    private final StatusOperacionalConteiner novoStatus;

    TipoMovimentacaoNavioEnum(String descricao,
                              TipoOperacaoConteiner tipoOperacaoConteiner,
                              String descricaoHistorico,
                              StatusOperacionalConteiner novoStatus) {
        this.descricao = descricao;
        this.tipoOperacaoConteiner = tipoOperacaoConteiner;
        this.descricaoHistorico = descricaoHistorico;
        this.novoStatus = novoStatus;
    }

    @JsonValue
    public String getDescricao() {
        return descricao;
    }

    public TipoOperacaoConteiner getTipoOperacaoConteiner() {
        return tipoOperacaoConteiner;
    }

    public String getDescricaoHistorico() {
        return descricaoHistorico;
    }

    public StatusOperacionalConteiner getNovoStatus() {
        return novoStatus;
    }

    @JsonCreator
    public static TipoMovimentacaoNavioEnum fromString(String valor) {
        if (valor == null) {
            return null;
        }
        String chaveNormalizada = valor.trim().toLowerCase(Locale.ROOT);
        TipoMovimentacaoNavioEnum porNome = NOME_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porNome != null) {
            return porNome;
        }
        TipoMovimentacaoNavioEnum porDescricao = DESCRICAO_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porDescricao != null) {
            return porDescricao;
        }
        throw new IllegalArgumentException("Valor inválido para TipoMovimentacaoNavioEnum: '" + valor + "'");
    }
}
