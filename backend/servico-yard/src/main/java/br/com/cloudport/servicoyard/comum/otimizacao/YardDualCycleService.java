package br.com.cloudport.servicoyard.comum.otimizacao;

import br.com.cloudport.servicoyard.comum.util.YardDistanceCalculator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Única implementação de dual-cycling do pátio.
 * Consolida OtimizadorDualCyclingServico e DualCycleOptimizationService,
 * eliminando as duplicações com métricas divergentes (Euclidiana vs Manhattan).
 *
 * Usa Manhattan — única métrica correta em pátios portuários.
 */
@Service
public class YardDualCycleService {

    public List<DualCyclePair> otimizar(
            List<YardPosition> pickups,
            List<YardPosition> dropoffs,
            DualCycleConfig config) {

        List<DualCyclePair> pairs = new ArrayList<>();
        List<YardPosition> dropoffsDisponiveis = new ArrayList<>(dropoffs);

        List<YardPosition> pickupsOrdenados = new ArrayList<>(pickups);

        for (YardPosition pickup : pickupsOrdenados) {
            DualCyclePair melhor = null;
            int menorDistancia = Integer.MAX_VALUE;

            for (YardPosition dropoff : dropoffsDisponiveis) {
                if (jaEmPair(dropoff, pairs)) {
                    continue;
                }
                int distancia = YardDistanceCalculator.manhattan(
                        pickup.getLinha(), pickup.getColuna(),
                        dropoff.getLinha(), dropoff.getColuna()
                );
                if (distancia <= config.getRaioMaximo() && distancia < menorDistancia) {
                    double economia = calcularEconomiaPercentual(pickup, dropoff, distancia);
                    if (economia >= config.getEconomiaMinima()) {
                        melhor = new DualCyclePair(pickup, dropoff, distancia, economia);
                        menorDistancia = distancia;
                    }
                }
            }

            if (melhor != null) {
                pairs.add(melhor);
            }
        }

        pairs.sort(Comparator.comparingDouble(DualCyclePair::getEconomia).reversed());
        return pairs;
    }

    private double calcularEconomiaPercentual(YardPosition pickup, YardPosition dropoff, int distanciaComPair) {
        int distPickup = YardDistanceCalculator.fromOrigin(pickup.getLinha(), pickup.getColuna());
        int distDropoff = YardDistanceCalculator.fromOrigin(dropoff.getLinha(), dropoff.getColuna());
        int distanciaIndividual = distPickup + distDropoff;
        if (distanciaIndividual == 0) {
            return 0;
        }
        return (1.0 - (double) distanciaComPair / distanciaIndividual) * 100.0;
    }

    private boolean jaEmPair(YardPosition pos, List<DualCyclePair> pairs) {
        return pairs.stream().anyMatch(p -> p.getDropoff().getId().equals(pos.getId()));
    }

    // ── DTOs internos ─────────────────────────────────────────────────────────

    public static class YardPosition {
        private final String id;
        private final int linha;
        private final int coluna;
        private final String tipo;

        public YardPosition(String id, int linha, int coluna, String tipo) {
            this.id = id;
            this.linha = linha;
            this.coluna = coluna;
            this.tipo = tipo;
        }

        public String getId() { return id; }
        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
        public String getTipo() { return tipo; }
    }

    public static class DualCyclePair {
        private final YardPosition pickup;
        private final YardPosition dropoff;
        private final int distancia;
        private final double economia;

        public DualCyclePair(YardPosition pickup, YardPosition dropoff, int distancia, double economia) {
            this.pickup = pickup;
            this.dropoff = dropoff;
            this.distancia = distancia;
            this.economia = economia;
        }

        public YardPosition getPickup() { return pickup; }
        public YardPosition getDropoff() { return dropoff; }
        public int getDistancia() { return distancia; }
        public double getEconomia() { return economia; }
    }

    public static class DualCycleConfig {
        private final int raioMaximo;
        private final double economiaMinima;

        public DualCycleConfig(int raioMaximo, double economiaMinima) {
            this.raioMaximo = raioMaximo;
            this.economiaMinima = economiaMinima;
        }

        public static DualCycleConfig padrao() {
            return new DualCycleConfig(10, 15.0);
        }

        public static DualCycleConfig semRestricao() {
            return new DualCycleConfig(Integer.MAX_VALUE, 0.0);
        }

        public int getRaioMaximo() { return raioMaximo; }
        public double getEconomiaMinima() { return economiaMinima; }
    }
}
