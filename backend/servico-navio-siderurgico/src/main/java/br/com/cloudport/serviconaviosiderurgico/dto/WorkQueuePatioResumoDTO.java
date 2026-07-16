package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;

public record WorkQueuePatioResumoDTO(
        Long id,
        String identificador,
        String agrupamento,
        Long visitaNavioId,
        String berco,
        Integer porao,
        String blocoZona,
        String pow,
        String poolOperacional,
        String equipamento,
        String status,
        Integer prioridadeOperacional,
        Integer totalOrdens,
        LocalDateTime atualizadoEm
) {

    public static WorkQueuePatioResumoDTO de(WorkQueuePatioDaVisitaDTO fila) {
        return new WorkQueuePatioResumoDTO(
                fila.id(),
                fila.identificador(),
                fila.agrupamento(),
                fila.visitaNavioId(),
                fila.berco(),
                fila.porao(),
                fila.blocoZona(),
                fila.pow(),
                fila.poolOperacional(),
                fila.equipamento(),
                fila.status(),
                fila.prioridadeOperacional(),
                fila.totalOrdens(),
                fila.atualizadoEm()
        );
    }
}
