package br.com.cloudport.servicogate.model.enums;

import java.util.Locale;

public enum TipoOcorrenciaOperador {
    ATRASO_EM_FILAS("Atraso em filas", NivelEvento.ALERTA),
    DOCUMENTACAO_PENDENTE("Documentação pendente", NivelEvento.ALERTA),
    INCIDENTE_SEGURANCA("Incidente de segurança", NivelEvento.CRITICA),
    MANUTENCAO_EQUIPAMENTO("Manutenção de equipamento", NivelEvento.OPERACIONAL),
    OPERACAO_NORMAL("Ocorrência operacional", NivelEvento.INFO);

    private final String descricao;
    private final NivelEvento nivelPadrao;

    TipoOcorrenciaOperador(String descricao, NivelEvento nivelPadrao) {
        this.descricao = descricao;
        this.nivelPadrao = nivelPadrao;
    }

    public String getDescricao() {
        return descricao;
    }

    public NivelEvento getNivelPadrao() {
        return nivelPadrao;
    }

    public static TipoOcorrenciaOperador fromCodigo(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("Tipo de ocorrência não informado");
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return TipoOcorrenciaOperador.valueOf(normalizado);
    }
}
