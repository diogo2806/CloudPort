package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;

public record ResumoIntegracaoNavioPatioDTO(
        Long visitaNavioId,
        int totalItens,
        long itensComReserva,
        long itensComOrdem,
        long itensSemReserva,
        long itensSemOrdem,
        long ordensEmExecucao,
        long ordensConcluidas,
        int totalAlertas,
        StatusIntegracaoPatio statusPredominante
) {}
