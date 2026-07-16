package br.com.cloudport.serviconaviosiderurgico.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OtimizacaoGlobalNavioPatioDTO(
        Long visitaNavioId,
        LocalDateTime geradoEm,
        int equipamentosConsiderados,
        int itensImportacaoConsiderados,
        int itensExportacaoConsiderados,
        int itensSemPosicaoReal,
        String status,
        List<String> alertas,
        Map<String, Object> planoOtimizado
) {
}
