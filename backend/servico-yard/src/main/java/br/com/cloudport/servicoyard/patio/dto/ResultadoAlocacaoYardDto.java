package br.com.cloudport.servicoyard.patio.dto;

public record ResultadoAlocacaoYardDto(
        Long containerId,
        String codigoContainer,
        String status,
        Integer linha,
        Integer coluna,
        Integer nivel,
        String motivo) {

    public static ResultadoAlocacaoYardDto alocado(PosicaoOtimizadaDto posicao) {
        return new ResultadoAlocacaoYardDto(posicao.getContainerId(), posicao.getCodigoContainer(),
                "ALOCADO", posicao.getLinha(), posicao.getColuna(), posicao.getNivel(), null);
    }

    public static ResultadoAlocacaoYardDto rejeitado(PosicaoOtimizadaDto posicao, String motivo) {
        return new ResultadoAlocacaoYardDto(posicao.getContainerId(), posicao.getCodigoContainer(),
                "REJEITADO", posicao.getLinha(), posicao.getColuna(), posicao.getNivel(), motivo);
    }
}
