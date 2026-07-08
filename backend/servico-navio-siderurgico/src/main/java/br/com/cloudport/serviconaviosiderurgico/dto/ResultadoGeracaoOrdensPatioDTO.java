package br.com.cloudport.serviconaviosiderurgico.dto;

import java.util.List;

public record ResultadoGeracaoOrdensPatioDTO(
        int totalOrdensCriadas,
        int totalItensIgnorados,
        int totalItensComErro,
        List<String> errosPorItem,
        List<String> alertas
) {}
