package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;

public record ResumoOperacionalNavioDTO(
        long totalItensPlanejados,
        long totalItensOperados,
        BigDecimal pesoPlanejado,
        BigDecimal pesoOperado,
        int percentualProgresso,
        long divergenciasPoraoPosicao,
        long itensBloqueados,
        Long tempoOperacaoMinutos
) {}
