package br.com.cloudport.servicoyard.comum.util;

/**
 * Distância Manhattan — única métrica correta para pátios portuários,
 * onde contêineres se movem exclusivamente ao longo de linhas e colunas.
 */
public final class YardDistanceCalculator {

    private YardDistanceCalculator() {}

    public static int manhattan(int l1, int c1, int l2, int c2) {
        return Math.abs(l1 - l2) + Math.abs(c1 - c2);
    }

    public static int fromOrigin(int linha, int coluna) {
        return Math.abs(linha) + Math.abs(coluna);
    }

    public static double manhattanDouble(Integer l1, Integer c1, Integer l2, Integer c2) {
        if (l1 == null || c1 == null || l2 == null || c2 == null) {
            return Double.MAX_VALUE;
        }
        return Math.abs(l1 - l2) + Math.abs(c1 - c2);
    }
}
