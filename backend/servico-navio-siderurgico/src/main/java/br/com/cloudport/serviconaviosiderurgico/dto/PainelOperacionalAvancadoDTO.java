package br.com.cloudport.serviconaviosiderurgico.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PainelOperacionalAvancadoDTO(
        Long visitaNavioId,
        LocalDateTime geradoEm,
        List<VisaoPoraoDTO> vesselView,
        List<VisaoBlocoPatioDTO> yardView,
        List<DetalheCheDTO> cheDetail,
        QuayMonitorDTO quayMonitor,
        List<DivergenciaOperacionalDTO> divergencias,
        List<GargaloOperacionalDTO> gargalos,
        ValidacaoEstruturalNavioDTO validacaoEstrutural
) {
    public record VisaoPoraoDTO(
            Integer porao,
            int itensPlanejados,
            int itensOperados,
            BigDecimal pesoPlanejadoToneladas,
            BigDecimal pesoOperadoToneladas,
            int alertas
    ) {
    }

    public record VisaoBlocoPatioDTO(
            String blocoZona,
            int reservasAtivas,
            int ordensPendentes,
            int ordensEmExecucao,
            int ordensConcluidas,
            int posicoesDivergentes
    ) {
    }

    public record DetalheCheDTO(
            String equipamento,
            String tipo,
            String statusOperacional,
            String workQueue,
            String pow,
            String poolOperacional,
            int jobs,
            int jobsEmExecucao,
            LocalDateTime telemetriaAtualizadaEm,
            Double latitude,
            Double longitude,
            Double heading,
            String posicaoMaisProxima,
            Long workInstructionAtualId
    ) {
    }

    public record QuayMonitorDTO(
            String berco,
            String fase,
            int movimentosPlanejados,
            int movimentosExecutados,
            int movimentosPendentes,
            int workQueuesAtivas,
            int equipamentosAlocados,
            BigDecimal movimentosPorHora,
            LocalDateTime inicioOperacao,
            LocalDateTime previsaoConclusao,
            String riscoOperacional
    ) {
    }

    public record DivergenciaOperacionalDTO(
            Long itemOperacaoNavioId,
            String codigoLote,
            String categoria,
            String planejado,
            String executado,
            String severidade,
            String mensagem
    ) {
    }

    public record GargaloOperacionalDTO(
            String codigo,
            String recurso,
            String severidade,
            int quantidadeAfetada,
            LocalDateTime previstoPara,
            String causa,
            String acaoRecomendada
    ) {
    }
}
