package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WorkQueuePatioDaVisitaDTO(
        Long id,
        String identificador,
        String agrupamento,
        Long visitaNavioId,
        String berco,
        Integer porao,
        String blocoZona,
        Integer sequenciaInicial,
        String pow,
        String poolOperacional,
        String equipamento,
        String status,
        Integer prioridadeOperacional,
        int totalOrdens,
        List<OrdemPatioDaVisitaDTO> jobList,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
