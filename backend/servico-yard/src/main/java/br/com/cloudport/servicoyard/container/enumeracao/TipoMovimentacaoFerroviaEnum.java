package br.com.cloudport.servicoyard.container.enumeracao;

import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum TipoMovimentacaoFerroviaEnum {
    DESCARGA_TREM(
            "Descarga do trem",
            TipoOperacaoConteiner.DESCARGA_TREM,
            "Descarga do trem concluída e contêiner disponível no pátio.",
            StatusOperacionalConteiner.ALOCADO
    ),
    CARGA_TREM(
            "Carga no trem",
            TipoOperacaoConteiner.CARGA_TREM,
            "Carga no trem concluída e contêiner encaminhado para o modal ferroviário.",
            StatusOperacionalConteiner.EM_TRANSFERENCIA
    );

    private static final Map<String, TipoMovimentacaoFerroviaEnum> NOME_PARA_ENUM_MAP;
    private static final Map<String, TipoMovimentacaoFerroviaEnum> DESCRICAO_PARA_ENUM_MAP;

    static {
        Map<String, TipoMovimentacaoFerroviaEnum> mapaNome = new HashMap<>();
        Map<String, TipoMovimentacaoFerroviaEnum> mapaDescricao = new HashMap<>();
        for (TipoMovimentacaoFerroviaEnum tipo : values()) {
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

    TipoMovimentacaoFerroviaEnum(String descricao,
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
    public static TipoMovimentacaoFerroviaEnum fromString(String valor) {
        if (valor == null) {
            return null;
        }
        String chaveNormalizada = valor.trim().toLowerCase(Locale.ROOT);
        TipoMovimentacaoFerroviaEnum porNome = NOME_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porNome != null) {
            return porNome;
        }
        TipoMovimentacaoFerroviaEnum porDescricao = DESCRICAO_PARA_ENUM_MAP.get(chaveNormalizada);
        if (porDescricao != null) {
            return porDescricao;
        }
        throw new IllegalArgumentException("Valor inválido para TipoMovimentacaoFerroviaEnum: '" + valor + "'");
    }
}
