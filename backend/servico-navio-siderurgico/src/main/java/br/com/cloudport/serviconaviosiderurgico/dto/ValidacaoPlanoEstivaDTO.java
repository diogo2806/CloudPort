package br.com.cloudport.serviconaviosiderurgico.dto;

import java.util.List;

public record ValidacaoPlanoEstivaDTO(
        PlanoEstivaNavioDTO plano,
        List<String> erros,
        List<String> alertas
) {}
