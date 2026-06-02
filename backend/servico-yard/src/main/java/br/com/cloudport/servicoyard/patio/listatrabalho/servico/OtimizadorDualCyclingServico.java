package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtimizadorDualCyclingServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;

    public OtimizadorDualCyclingServico(OrdemTrabalhoPatioRepositorio ordemRepositorio) {
        this.ordemRepositorio = ordemRepositorio;
    }

    @Transactional(readOnly = true)
    public AnaliseDualCyclingDto analisarPairingsPotenciais() {
        List<OrdemTrabalhoPatio> ordensEntrada = obterOrdensEntrada();
        List<OrdemTrabalhoPatio> ordensSaida = obterOrdensSaida();

        List<PairOrdensTrabalhDto> pairsOtimizados = gerarPairsOtimizados(ordensEntrada, ordensSaida);

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

        Map<String, List<OrdemTrabalhoPatio>> blocosEntrada = agruparPorBloco(ordensEntrada);
        Map<String, List<OrdemTrabalhoPatio>> blocosSaida = agruparPorBloco(ordensSaida);

        List<PairOrdensTrabalhDto> pairs = new ArrayList<>();

        for (Map.Entry<String, List<OrdemTrabalhoPatio>> entrada : blocosEntrada.entrySet()) {
            String blocoAtual = entrada.getKey();
            List<OrdemTrabalhoPatio> ordensBlockoAtual = entrada.getValue();

            List<String> blocosPotenciais = obterBlocosAdjacentesEProximo(
                    blocoAtual,
                    raioAdjacencia != null ? raioAdjacencia : 10
            );

            for (OrdemTrabalhoPatio ordemEntrada : ordensBlockoAtual) {
                PairOrdensTrabalhDto melhorPair = null;
                double menorDistancia = Double.MAX_VALUE;

                for (String blocoPotencial : blocosPotenciais) {
                    List<OrdemTrabalhoPatio> ordensBlocoSaida = blocosSaida.getOrDefault(
                            blocoPotencial,
                            new ArrayList<>()
                    );

                    for (OrdemTrabalhoPatio ordemSaida : ordensBlocoSaida) {
                        if (jaFoiPaireada(ordemSaida, pairs)) {
                            continue;
                        }

                        double distanciaRetorno = calcularDistanciaRetorno(ordemEntrada, ordemSaida);

                        if (distanciaRetorno < menorDistancia) {
                            menorDistancia = distanciaRetorno;
                            melhorPair = new PairOrdensTrabalhDto(
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

                                    blocoAtual,
                                    blocoPotencial,
                                    distanciaRetorno,
                                    calcularPontucaoQualidade(ordemEntrada, ordemSaida)
                            );
                        }
                    }
                }

                if (melhorPair != null) {
                    pairs.add(melhorPair);
                }
            }
        }

        return pairs.stream()
                .sorted(Comparator.comparing(PairOrdensTrabalhDto::getPontuacao).reversed())
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

    private Map<String, List<OrdemTrabalhoPatio>> agruparPorBloco(List<OrdemTrabalhoPatio> ordens) {
        return ordens.stream()
                .collect(Collectors.groupingBy(
                        ordem -> extrairBlocoDesCoordenadas(
                                ordem.getLinhaDestino(),
                                ordem.getColunaDestino()
                        )
                ));
    }

    private String extrairBlocoDesCoordenadas(Integer linha, Integer coluna) {
        int blocoLinha = (linha != null ? linha : 0) / 10;
        int blocoColuna = (coluna != null ? coluna : 0) / 10;
        return "BLOCO_" + blocoLinha + "_" + blocoColuna;
    }

    private List<String> obterBlocosAdjacentesEProximo(String blocoAtual, Integer raio) {
        List<String> blocos = new ArrayList<>();
        blocos.add(blocoAtual);

        String[] parts = blocoAtual.split("_");
        if (parts.length == 3) {
            try {
                int linha = Integer.parseInt(parts[1]);
                int coluna = Integer.parseInt(parts[2]);

                for (int i = -raio; i <= raio; i += 10) {
                    for (int j = -raio; j <= raio; j += 10) {
                        if (i == 0 && j == 0) continue;
                        int novaLinha = linha + i;
                        int novaColuna = coluna + j;
                        if (novaLinha >= 0 && novaColuna >= 0) {
                            blocos.add("BLOCO_" + novaLinha + "_" + novaColuna);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // Ignorar erro de parsing
            }
        }

        return blocos;
    }

    private double calcularDistanciaRetorno(OrdemTrabalhoPatio ordemEntrada,
                                             OrdemTrabalhoPatio ordemSaida) {
        int deltaLinha = ordemEntrada.getLinhaDestino() - ordemSaida.getLinhaDestino();
        int deltaColuna = ordemEntrada.getColunaDestino() - ordemSaida.getColunaDestino();

        return Math.sqrt(deltaLinha * deltaLinha + deltaColuna * deltaColuna);
    }

    private double calcularPontucaoQualidade(OrdemTrabalhoPatio ordemEntrada,
                                              OrdemTrabalhoPatio ordemSaida) {
        double distanciaRetorno = calcularDistanciaRetorno(ordemEntrada, ordemSaida);
        double proximidade = 100 - (distanciaRetorno * 2);

        boolean mesmoDestino = Objects.equals(
                ordemEntrada.getDestino(),
                ordemSaida.getDestino()
        );
        double bonusDestino = mesmoDestino ? 50 : 0;

        return Math.max(0, proximidade + bonusDestino);
    }

    private List<PairOrdensTrabalhDto> gerarPairsOtimizados(List<OrdemTrabalhoPatio> ordensEntrada,
                                                             List<OrdemTrabalhoPatio> ordensSaida) {
        return gerarPairs(10);
    }

    private double calcularDistanciaTotal(List<PairOrdensTrabalhDto> pairs) {
        return pairs.stream()
                .mapToDouble(PairOrdensTrabalhDto::getDistanciaRetorno)
                .sum();
    }

    private double calcularDistanciaIndividual(List<OrdemTrabalhoPatio> ordensEntrada,
                                                List<OrdemTrabalhoPatio> ordensSaida) {
        double distancia = 0;

        for (OrdemTrabalhoPatio ordem : ordensEntrada) {
            distancia += Math.abs(ordem.getLinhaDestino()) + Math.abs(ordem.getColunaDestino());
        }

        for (OrdemTrabalhoPatio ordem : ordensSaida) {
            distancia += Math.abs(ordem.getLinhaDestino()) + Math.abs(ordem.getColunaDestino());
        }

        return distancia;
    }

    private boolean jaFoiPaireada(OrdemTrabalhoPatio ordem, List<PairOrdensTrabalhDto> pairs) {
        return pairs.stream()
                .anyMatch(p -> p.getIdOrdemSaida().equals(ordem.getId()) ||
                        p.getIdOrdemEntrada().equals(ordem.getId()));
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
