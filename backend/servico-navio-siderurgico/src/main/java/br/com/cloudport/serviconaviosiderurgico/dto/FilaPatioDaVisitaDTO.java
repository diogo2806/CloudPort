package br.com.cloudport.serviconaviosiderurgico.dto;

import java.util.List;

public record FilaPatioDaVisitaDTO(
        String identificador,
        String agrupamento,
        Long visitaNavioId,
        String berco,
        String blocoZona,
        Integer sequenciaInicial,
        String status,
        long totalOrdens,
        List<OrdemPatioDaVisitaDTO> ordens
) {
}
