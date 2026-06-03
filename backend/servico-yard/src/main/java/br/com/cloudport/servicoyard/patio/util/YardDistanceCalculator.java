package br.com.cloudport.servicoyard.patio.util;

public final class YardDistanceCalculator {

    private YardDistanceCalculator() {
    }

    public static int manhattan(Integer linhaOrigem, Integer colunaOrigem,
                                Integer linhaDestino, Integer colunaDestino) {
        if (linhaOrigem == null || colunaOrigem == null || linhaDestino == null || colunaDestino == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(linhaOrigem - linhaDestino) + Math.abs(colunaOrigem - colunaDestino);
    }

    public static int fromOrigin(Integer linha, Integer coluna) {
        if (linha == null || coluna == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(linha) + Math.abs(coluna);
    }
}
