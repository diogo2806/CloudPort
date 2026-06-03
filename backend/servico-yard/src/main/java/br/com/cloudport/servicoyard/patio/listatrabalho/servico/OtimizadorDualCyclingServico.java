package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCycleConfig;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCyclePair;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.YardPosition;
import br.com.cloudport.servicoyard.comum.util.YardDistanceCalculator;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtimizadorDualCyclingServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final YardDualCycleService dualCycleService;

    public OtimizadorDualCyclingServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                         YardDualCycleService dualCycleService) {
        this.ordemRepositorio = ordemRepositorio;
        this.dualCycleService = dualCycleService;
    }

    @Transactional(readOnly = true)
    public AnaliseDualCyclingDto analisarPairingsPotenciais() {
        List<OrdemTrabalhoPatio> ordensEntrada = obterOrdensEntrada();
        List<OrdemTrabalhoPatio> ordensSaida = obterOrdensSaida();

        List<PairOrdensTrabalhDto> pairsOtimizados = gerarPairs(10);

        double distanciaTotal = calcularDistanciaTotal(pairsOtimizados);
        double distanciaIndividual = calcularDistanciaIndividual(ordensEntrada, ordensSaida);
        double economia = distanciaIndividual - distanciaTotal;
        double percentualEconomia = (economia / distanciaIndividual) * 100;

        return new AnaliseDualCyclingDto(
                pairsOtimizados,
                distanciaIndividual,
                distanciaTotal,
                economia,
                percentualEconomia,
                ordensEntrada.size() + ordensSaida.size()
        );
    }

    @Transactional(readOnly = true)
    public List<PairOrdensTrabalhDto> gerarPairs(Integer raioAdjacencia) {
        List<OrdemTrabalhoPatio> ordensEntrada = obterOrdensEntrada();
        List<OrdemTrabalhoPatio> ordensSaida = obterOrdensSaida();

        List<YardPosition> pickups = ordemParaYardPosition(ordensEntrada, TipoMovimentoPatio.ALOCACAO);
        List<YardPosition> dropoffs = ordemParaYardPosition(ordensSaida, TipoMovimentoPatio.REMOCAO);

        int raio = raioAdjacencia != null ? raioAdjacencia : 10;
        List<DualCyclePair> pairs = dualCycleService.otimizar(pickups, dropoffs,
                new DualCycleConfig(raio, 0.0));

        Map<String, OrdemTrabalhoPatio> entradaPorId = ordensEntrada.stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), o -> o));
        Map<String, OrdemTrabalhoPatio> saidaPorId = ordensSaida.stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), o -> o));

        return pairs.stream().map(pair -> {
            OrdemTrabalhoPatio oe = entradaPorId.get(pair.getPickup().getId());
            OrdemTrabalhoPatio os = saidaPorId.get(pair.getDropoff().getId());
            if (oe == null || os == null) return null;
            return new PairOrdensTrabalhDto(
                    oe.getId(), oe.getCodigoConteiner(), oe.getLinhaDestino(), oe.getColunaDestino(),
                    TipoMovimentoPatio.ALOCACAO,
                    os.getId(), os.getCodigoConteiner(), os.getLinhaDestino(), os.getColunaDestino(),
                    TipoMovimentoPatio.REMOCAO,
                    "BLOCO_" + (oe.getLinhaDestino() / 10) + "_" + (oe.getColunaDestino() / 10),
                    "BLOCO_" + (os.getLinhaDestino() / 10) + "_" + (os.getColunaDestino() / 10),
                    pair.getDistancia(),
                    pair.getEconomia()
            );
        }).filter(Objects::nonNull)
          .sorted(Comparator.comparing(PairOrdensTrabalhDto::getPontuacao).reversed())
          .collect(Collectors.toList());
    }

    private List<YardPosition> ordemParaYardPosition(List<OrdemTrabalhoPatio> ordens,
                                                       TipoMovimentoPatio tipo) {
        return ordens.stream()
                .map(o -> new YardPosition(
                        o.getId().toString(),
                        o.getLinhaDestino() != null ? o.getLinhaDestino() : 0,
                        o.getColunaDestino() != null ? o.getColunaDestino() : 0,
                        tipo.name()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatio> obterSequenciaOtimizadaComDualCycling() {
        List<PairOrdensTrabalhDto> pairs = gerarPairs(10);

        if (pairs.isEmpty()) {
            return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE);
        }

        List<OrdemTrabalhoPatio> resultado = new ArrayList<>();
        List<Long> idsProcessados = new ArrayList<>();

        for (PairOrdensTrabalhDto pair : pairs) {
            OrdemTrabalhoPatio ordemEntrada = ordemRepositorio.findById(pair.getIdOrdemEntrada())
                    .orElse(null);
            OrdemTrabalhoPatio ordemSaida = ordemRepositorio.findById(pair.getIdOrdemSaida())
                    .orElse(null);

            if (ordemEntrada != null && ordemSaida != null) {
                resultado.add(ordemEntrada);
                resultado.add(ordemSaida);
                idsProcessados.add(ordemEntrada.getId());
                idsProcessados.add(ordemSaida.getId());
            }
        }

        List<OrdemTrabalhoPatio> ordensRestantes = ordemRepositorio
                .findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE).stream()
                .filter(ordem -> !idsProcessados.contains(ordem.getId()))
                .collect(Collectors.toList());

        resultado.addAll(ordensRestantes);
        return resultado;
    }

    private List<OrdemTrabalhoPatio> obterOrdensEntrada() {
        return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE)
                .stream()
                .filter(ordem -> ordem.getTipoMovimento() == TipoMovimentoPatio.ALOCACAO)
                .collect(Collectors.toList());
    }

    private List<OrdemTrabalhoPatio> obterOrdensSaida() {
        return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE)
                .stream()
                .filter(ordem -> ordem.getTipoMovimento() == TipoMovimentoPatio.REMOCAO)
                .collect(Collectors.toList());
    }

    private double calcularDistanciaTotal(List<PairOrdensTrabalhDto> pairs) {
        return pairs.stream().mapToDouble(PairOrdensTrabalhDto::getDistanciaRetorno).sum();
    }

    private double calcularDistanciaIndividual(List<OrdemTrabalhoPatio> ordensEntrada,
                                                List<OrdemTrabalhoPatio> ordensSaida) {
        return ordensEntrada.stream()
                .mapToDouble(o -> YardDistanceCalculator.fromOrigin(
                        o.getLinhaDestino() != null ? o.getLinhaDestino() : 0,
                        o.getColunaDestino() != null ? o.getColunaDestino() : 0))
                .sum()
               + ordensSaida.stream()
                .mapToDouble(o -> YardDistanceCalculator.fromOrigin(
                        o.getLinhaDestino() != null ? o.getLinhaDestino() : 0,
                        o.getColunaDestino() != null ? o.getColunaDestino() : 0))
                .sum();
    }

    public static class AnaliseDualCyclingDto {
        private List<PairOrdensTrabalhDto> pairsOtimizados;
        private double distanciaIndividual;
        private double distanciaComPairing;
        private double economiaKm;
        private double percentualEconomia;
        private int totalOrdens;

        public AnaliseDualCyclingDto(List<PairOrdensTrabalhDto> pairsOtimizados,
                                      double distanciaIndividual,
                                      double distanciaComPairing,
                                      double economiaKm,
                                      double percentualEconomia,
                                      int totalOrdens) {
            this.pairsOtimizados = pairsOtimizados;
            this.distanciaIndividual = distanciaIndividual;
            this.distanciaComPairing = distanciaComPairing;
            this.economiaKm = economiaKm;
            this.percentualEconomia = percentualEconomia;
            this.totalOrdens = totalOrdens;
        }

        public List<PairOrdensTrabalhDto> getPairsOtimizados() { return pairsOtimizados; }
        public double getDistanciaIndividual() { return distanciaIndividual; }
        public double getDistanciaComPairing() { return distanciaComPairing; }
        public double getEconomiaKm() { return economiaKm; }
        public double getPercentualEconomia() { return percentualEconomia; }
        public int getTotalOrdens() { return totalOrdens; }
    }

    public static class PairOrdensTrabalhDto {
        private Long idOrdemEntrada;
        private String codigoConteinerEntrada;
        private Integer linhaDestEntrada;
        private Integer colunaDestEntrada;
        private TipoMovimentoPatio tipoMovimentoEntrada;

        private Long idOrdemSaida;
        private String codigoConteinerSaida;
        private Integer linhaDestSaida;
        private Integer colunaDestSaida;
        private TipoMovimentoPatio tipoMovimentoSaida;

        private String blocoEntrada;
        private String blocoSaida;
        private double distanciaRetorno;
        private double pontuacao;

        public PairOrdensTrabalhDto(
                Long idOrdemEntrada,
                String codigoConteinerEntrada,
                Integer linhaDestEntrada,
                Integer colunaDestEntrada,
                TipoMovimentoPatio tipoMovimentoEntrada,

                Long idOrdemSaida,
                String codigoConteinerSaida,
                Integer linhaDestSaida,
                Integer colunaDestSaida,
                TipoMovimentoPatio tipoMovimentoSaida,

                String blocoEntrada,
                String blocoSaida,
                double distanciaRetorno,
                double pontuacao) {
            this.idOrdemEntrada = idOrdemEntrada;
            this.codigoConteinerEntrada = codigoConteinerEntrada;
            this.linhaDestEntrada = linhaDestEntrada;
            this.colunaDestEntrada = colunaDestEntrada;
            this.tipoMovimentoEntrada = tipoMovimentoEntrada;

            this.idOrdemSaida = idOrdemSaida;
            this.codigoConteinerSaida = codigoConteinerSaida;
            this.linhaDestSaida = linhaDestSaida;
            this.colunaDestSaida = colunaDestSaida;
            this.tipoMovimentoSaida = tipoMovimentoSaida;

            this.blocoEntrada = blocoEntrada;
            this.blocoSaida = blocoSaida;
            this.distanciaRetorno = distanciaRetorno;
            this.pontuacao = pontuacao;
        }

        public Long getIdOrdemEntrada() { return idOrdemEntrada; }
        public String getCodigoConteinerEntrada() { return codigoConteinerEntrada; }
        public Integer getLinhaDestEntrada() { return linhaDestEntrada; }
        public Integer getColunaDestEntrada() { return colunaDestEntrada; }
        public TipoMovimentoPatio getTipoMovimentoEntrada() { return tipoMovimentoEntrada; }

        public Long getIdOrdemSaida() { return idOrdemSaida; }
        public String getCodigoConteinerSaida() { return codigoConteinerSaida; }
        public Integer getLinhaDestSaida() { return linhaDestSaida; }
        public Integer getColunaDestSaida() { return colunaDestSaida; }
        public TipoMovimentoPatio getTipoMovimentoSaida() { return tipoMovimentoSaida; }

        public String getBlocoEntrada() { return blocoEntrada; }
        public String getBlocoSaida() { return blocoSaida; }
        public double getDistanciaRetorno() { return distanciaRetorno; }
        public double getPontuacao() { return pontuacao; }
    }
}
