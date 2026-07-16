package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusConsultaYard;
import java.util.List;
import java.util.Objects;

public record ResultadoConsultaYardDTO<T>(
        List<T> dados,
        StatusConsultaYard status,
        boolean confirmado,
        String fonte,
        String motivoDegradacao
) {

    public ResultadoConsultaYardDTO {
        dados = dados == null ? List.of() : List.copyOf(dados);
        status = Objects.requireNonNull(status, "O status da consulta ao Yard e obrigatorio.");
        fonte = Objects.requireNonNull(fonte, "A fonte da consulta ao Yard e obrigatoria.");
    }

    public static <T> ResultadoConsultaYardDTO<T> confirmada(List<T> dados) {
        return new ResultadoConsultaYardDTO<>(
                dados,
                StatusConsultaYard.CONFIRMADA,
                true,
                "YARD",
                null
        );
    }

    public static <T> ResultadoConsultaYardDTO<T> degradada(
            List<T> dados,
            String motivoDegradacao) {
        return new ResultadoConsultaYardDTO<>(
                dados,
                StatusConsultaYard.DEGRADADA,
                false,
                "DERIVACAO_LOCAL_CONTINGENCIA",
                motivoDegradacao
        );
    }
}
