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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtimizadorDualCyclingServico {

    private static final int RAIO_ADJACENCIA_PADRAO = 10;
    private static final String BLOCO_NAO_IDENTIFICADO = "BLOCO_NAO_IDENTIFICADO";

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final YardDualCycleService dualCycleService;

    public OtimizadorDualCyclingServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                         YardDualCycleService dualCycleService) {
        this.ordemRepositorio = ordemRepositorio;
        this.dualCycleService = dualCycleService;
    }

    @Transactional(readOnly = true)
    public AnaliseDualCyclingDto analisarPairingsPotenciais() {
        List<OrdemTrabalhoPatio> ordensPendentes = obterOrdensPendentes();
        List<OrdemTrabalhoPatio> ordensEntrada = filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.ALOCACAO);
        List<OrdemTrabalhoPatio> ordensSaida = filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.REMOCAO);
        List<PairOrdensTrabalhoDto> pairsOtimizados = gerarPairs(
                ordensEntrada, ordensSaida, RAIO_ADJACENCIA_PADRAO);

        double distanciaTotal = calcularDistanciaTotal(pairsOtimizados);
        double distanciaIndividual = calcularDistanciaIndividual(ordensEntrada, ordensSaida);
        double economia = distanciaIndividual - distanciaTotal;
        double percentualEconomia = distanciaIndividual > 0
                ? (economia / distanciaIndividual) * 100.0
                : 0.0;

        return new AnaliseDualCyclingDto(
                pairsOtimizados,
                distanciaIndividual,
                distanciaTotal,
                economia,
                percentualEconomia,
                ordensPendentes.size());
    }

    @Transactional(readOnly = true)
    public List<PairOrdensTrabalhoDto> gerarPairs(Integer raioAdjacencia) {
        List<OrdemTrabalhoPatio> ordensPendentes = obterOrdensPendentes();
        return gerarPairs(
                filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.ALOCACAO),
                filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.REMOCAO),
                raioAdjacencia != null ? raioAdjacencia : RAIO_ADJACENCIA_PADRAO);
    }

    private List<PairOrdensTrabalhoDto> gerarPairs(List<OrdemTrabalhoPatio> ordensEntrada,
                                                     List<OrdemTrabalhoPatio> ordensSaida,
                                                     int raioAdjacencia) {
        List<YardPosition> pickups = ordemParaYardPosition(ordensEntrada, TipoMovimentoPatio.ALOCACAO);
        List<YardPosition> dropoffs = ordemParaYardPosition(ordensSaida, TipoMovimentoPatio.REMOCAO);
        List<DualCyclePair> pairs = dualCycleService.otimizar(
                pickups,
                dropoffs,
                new DualCycleConfig(raioAdjacencia, 0.0));

        Map<String, OrdemTrabalhoPatio> entradaPorId = ordensEntrada.stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), o -> o));
        Map<String, OrdemTrabalhoPatio> saidaPorId = ordensSaida.stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), o -> o));

        return pairs.stream()
                .map(pair -> criarPair(pair, entradaPorId, saidaPorId))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PairOrdensTrabalhoDto::getPontuacao).reversed())
                .collect(Collectors.toList());
    }

    private PairOrdensTrabalhoDto criarPair(DualCyclePair pair,
                                             Map<String, OrdemTrabalhoPatio> entradaPorId,
                                             Map<String, OrdemTrabalhoPatio> saidaPorId) {
        OrdemTrabalhoPatio ordemEntrada = entradaPorId.get(pair.getPickup().getId());
        OrdemTrabalhoPatio ordemSaida = saidaPorId.get(pair.getDropoff().getId());
        if (ordemEntrada == null || ordemSaida == null) {
            return null;
        }

        return new PairOrdensTrabalhoDto(
                ordemEntrada.getId(),
                ordemEntrada.getCodigoConteiner(),
                ordemEntrada.getLinhaDestino(),
                ordemEntrada.getColunaDestino(),
                TipoMovimentoPatio.ALOCACAO,
                ordemSaida.getId(),
                ordemSaida.getCodigoConteiner(),
                ordemSaida.getLinhaDestino(),
                ordemSaida.getColunaDestino(),
                TipoMovimentoPatio.REMOCAO,
                identificarBloco(ordemEntrada),
                identificarBloco(ordemSaida),
                pair.getDistancia(),
                pair.getEconomia());
    }

    private String identificarBloco(OrdemTrabalhoPatio ordem) {
        if (ordem.getLinhaDestino() == null || ordem.getColunaDestino() == null) {
            return BLOCO_NAO_IDENTIFICADO;
        }
        return "BLOCO_" + (ordem.getLinhaDestino() / 10) + "_" + (ordem.getColunaDestino() / 10);
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
        List<OrdemTrabalhoPatio> ordensPendentes = obterOrdensPendentes();
        List<PairOrdensTrabalhoDto> pairs = gerarPairs(
                filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.ALOCACAO),
                filtrarPorTipo(ordensPendentes, TipoMovimentoPatio.REMOCAO),
                RAIO_ADJACENCIA_PADRAO);

        if (pairs.isEmpty()) {
            return ordensPendentes;
        }

        Map<Long, OrdemTrabalhoPatio> ordensPorId = ordensPendentes.stream()
                .collect(Collectors.toMap(
                        OrdemTrabalhoPatio::getId,
                        ordem -> ordem,
                        (existente, duplicada) -> existente,
                        LinkedHashMap::new));
        List<OrdemTrabalhoPatio> resultado = new ArrayList<>();

        for (PairOrdensTrabalhoDto pair : pairs) {
            OrdemTrabalhoPatio ordemEntrada = ordensPorId.remove(pair.getIdOrdemEntrada());
            OrdemTrabalhoPatio ordemSaida = ordensPorId.remove(pair.getIdOrdemSaida());
            if (ordemEntrada != null && ordemSaida != null) {
                resultado.add(ordemEntrada);
                resultado.add(ordemSaida);
            } else {
                if (ordemEntrada != null) {
                    ordensPorId.put(ordemEntrada.getId(), ordemEntrada);
                }
                if (ordemSaida != null) {
                    ordensPorId.put(ordemSaida.getId(), ordemSaida);
                }
            }
        }

        resultado.addAll(ordensPorId.values());
        return resultado;
    }

    private List<OrdemTrabalhoPatio> obterOrdensPendentes() {
        return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(StatusOrdemTrabalhoPatio.PENDENTE);
    }

    private List<OrdemTrabalhoPatio> filtrarPorTipo(List<OrdemTrabalhoPatio> ordens,
                                                      TipoMovimentoPatio tipo) {
        return ordens.stream()
                .filter(ordem -> ordem.getTipoMovimento() == tipo)
                .collect(Collectors.toList());
    }

    private double calcularDistanciaTotal(List<PairOrdensTrabalhoDto> pairs) {
        return pairs.stream().mapToDouble(PairOrdensTrabalhoDto::getDistanciaRetorno).sum();
    }

    private double calcularDistanciaIndividual(List<OrdemTrabalhoPatio> ordensEntrada,
                                                List<OrdemTrabalhoPatio> ordensSaida) {
        return ordensEntrada.stream()
                .mapToDouble(this::calcularDistanciaDaOrigem)
                .sum()
                + ordensSaida.stream()
                .mapToDouble(this::calcularDistanciaDaOrigem)
                .sum();
    }

    private double calcularDistanciaDaOrigem(OrdemTrabalhoPatio ordem) {
        return YardDistanceCalculator.fromOrigin(
                ordem.getLinhaDestino() != null ? ordem.getLinhaDestino() : 0,
                ordem.getColunaDestino() != null ? ordem.getColunaDestino() : 0);
    }

    public static class AnaliseDualCyclingDto {
        private final List<PairOrdensTrabalhoDto> pairsOtimizados;
        private final double distanciaIndividual;
        private final double distanciaComPairing;
        private final double economiaKm;
        private final double percentualEconomia;
        private final int totalOrdens;

        public AnaliseDualCyclingDto(List<PairOrdensTrabalhoDto> pairsOtimizados,
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

        public List<PairOrdensTrabalhoDto> getPairsOtimizados() {
            return pairsOtimizados;
        }

        public double getDistanciaIndividual() {
            return distanciaIndividual;
        }

        public double getDistanciaComPairing() {
            return distanciaComPairing;
        }

        public double getEconomiaKm() {
            return economiaKm;
        }

        public double getPercentualEconomia() {
            return percentualEconomia;
        }

        public int getTotalOrdens() {
            return totalOrdens;
        }
    }

    public static class PairOrdensTrabalhoDto {
        private final Long idOrdemEntrada;
        private final String codigoConteinerEntrada;
        private final Integer linhaDestEntrada;
        private final Integer colunaDestEntrada;
        private final TipoMovimentoPatio tipoMovimentoEntrada;
        private final Long idOrdemSaida;
        private final String codigoConteinerSaida;
        private final Integer linhaDestSaida;
        private final Integer colunaDestSaida;
        private final TipoMovimentoPatio tipoMovimentoSaida;
        private final String blocoEntrada;
        private final String blocoSaida;
        private final double distanciaRetorno;
        private final double pontuacao;

        public PairOrdensTrabalhoDto(Long idOrdemEntrada,
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

        public Long getIdOrdemEntrada() {
            return idOrdemEntrada;
        }

        public String getCodigoConteinerEntrada() {
            return codigoConteinerEntrada;
        }

        public Integer getLinhaDestEntrada() {
            return linhaDestEntrada;
        }

        public Integer getColunaDestEntrada() {
            return colunaDestEntrada;
        }

        public TipoMovimentoPatio getTipoMovimentoEntrada() {
            return tipoMovimentoEntrada;
        }

        public Long getIdOrdemSaida() {
            return idOrdemSaida;
        }

        public String getCodigoConteinerSaida() {
            return codigoConteinerSaida;
        }

        public Integer getLinhaDestSaida() {
            return linhaDestSaida;
        }

        public Integer getColunaDestSaida() {
            return colunaDestSaida;
        }

        public TipoMovimentoPatio getTipoMovimentoSaida() {
            return tipoMovimentoSaida;
        }

        public String getBlocoEntrada() {
            return blocoEntrada;
        }

        public String getBlocoSaida() {
            return blocoSaida;
        }

        public double getDistanciaRetorno() {
            return distanciaRetorno;
        }

        public double getPontuacao() {
            return pontuacao;
        }
    }
}
