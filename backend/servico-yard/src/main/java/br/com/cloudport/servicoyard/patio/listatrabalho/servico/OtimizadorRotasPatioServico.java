package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCycleConfig;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.DualCyclePair;
import br.com.cloudport.servicoyard.comum.otimizacao.YardDualCycleService.YardPosition;
import br.com.cloudport.servicoyard.comum.util.YardDistanceCalculator;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtimizadorRotasPatioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final YardDualCycleService dualCycleService;

    public OtimizadorRotasPatioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                        YardDualCycleService dualCycleService) {
        this.ordemRepositorio = ordemRepositorio;
        this.dualCycleService = dualCycleService;
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatio> otimizarRota() {
        List<OrdemTrabalhoPatio> ordensPendentes = ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(
                StatusOrdemTrabalhoPatio.PENDENTE);

        if (ordensPendentes.isEmpty()) {
            return ordensPendentes;
        }

        return aplicarNearestNeighbor(ordensPendentes);
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatio> otimizarRotaComProximidade() {
        List<OrdemTrabalhoPatio> ordensPendentes = ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(
                StatusOrdemTrabalhoPatio.PENDENTE);

        if (ordensPendentes.isEmpty()) {
            return ordensPendentes;
        }

        List<OrdemTrabalhoPatio> otimizadas = aplicarNearestNeighbor(ordensPendentes);
        return aplicarDualCycling(otimizadas);
    }

    private List<OrdemTrabalhoPatio> aplicarNearestNeighbor(List<OrdemTrabalhoPatio> ordens) {
        List<OrdemTrabalhoPatio> resultado = new ArrayList<>();
        List<OrdemTrabalhoPatio> pendentes = new ArrayList<>(ordens);

        if (pendentes.isEmpty()) {
            return resultado;
        }

        PosicaoPatio ultimaPosicao = obterPosicaoInicial(pendentes);
        resultado.add(pendentes.remove(0));

        while (!pendentes.isEmpty()) {
            int proximoIndice = encontrarProximoMaisProximo(pendentes, ultimaPosicao);
            OrdemTrabalhoPatio proximaOrdem = pendentes.remove(proximoIndice);
            resultado.add(proximaOrdem);
            ultimaPosicao = obterPosicaoDestino(proximaOrdem);
        }

        return resultado;
    }

    private List<OrdemTrabalhoPatio> aplicarDualCycling(List<OrdemTrabalhoPatio> ordens) {
        List<YardPosition> pickups = ordens.stream()
                .map(o -> new YardPosition(
                        o.getId().toString(),
                        o.getLinhaDestino() != null ? o.getLinhaDestino() : 0,
                        o.getColunaDestino() != null ? o.getColunaDestino() : 0,
                        o.getTipoMovimento() != null ? o.getTipoMovimento().name() : ""))
                .collect(Collectors.toList());

        List<DualCyclePair> pairs = dualCycleService.otimizar(pickups, new ArrayList<>(),
                DualCycleConfig.semRestricao());

        Map<String, OrdemTrabalhoPatio> porId = ordens.stream()
                .collect(Collectors.toMap(o -> o.getId().toString(), o -> o));

        List<OrdemTrabalhoPatio> resultado = new ArrayList<>();
        for (DualCyclePair pair : pairs) {
            OrdemTrabalhoPatio o = porId.remove(pair.getPickup().getId());
            if (o != null) resultado.add(o);
        }
        resultado.addAll(porId.values());
        return resultado;
    }

    private int encontrarProximoMaisProximo(List<OrdemTrabalhoPatio> pendentes, PosicaoPatio posicaoAtual) {
        int proximoIndice = 0;
        double menorDistancia = Double.MAX_VALUE;

        for (int i = 0; i < pendentes.size(); i++) {
            OrdemTrabalhoPatio ordem = pendentes.get(i);
            PosicaoPatio posicaoConteiner = obterPosicaoAtual(ordem);

            double distancia = calcularDistancia(posicaoAtual, posicaoConteiner);

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                proximoIndice = i;
            }
        }

        return proximoIndice;
    }

    private double calcularDistancia(PosicaoPatio p1, PosicaoPatio p2) {
        if (p1 == null || p2 == null) {
            return Double.MAX_VALUE;
        }
        return YardDistanceCalculator.manhattan(p1.getLinha(), p1.getColuna(), p2.getLinha(), p2.getColuna());
    }

    private PosicaoPatio obterPosicaoAtual(OrdemTrabalhoPatio ordem) {
        if (ordem.getConteiner() != null && ordem.getConteiner().getPosicao() != null) {
            return ordem.getConteiner().getPosicao();
        }
        return new PosicaoPatio(null, 0, 0, "");
    }

    private PosicaoPatio obterPosicaoDestino(OrdemTrabalhoPatio ordem) {
        return new PosicaoPatio(null, ordem.getLinhaDestino(), ordem.getColunaDestino(),
                ordem.getCamadaDestino());
    }

    private PosicaoPatio obterPosicaoInicial(List<OrdemTrabalhoPatio> ordens) {
        if (ordens.isEmpty()) {
            return new PosicaoPatio(null, 0, 0, "");
        }
        return obterPosicaoAtual(ordens.get(0));
    }

    public double calcularDistanciaTotal(List<OrdemTrabalhoPatio> ordens) {
        double total = 0;
        PosicaoPatio posicaoAtual = obterPosicaoInicial(ordens);

        for (OrdemTrabalhoPatio ordem : ordens) {
            PosicaoPatio proxima = obterPosicaoAtual(ordem);
            total += calcularDistancia(posicaoAtual, proxima);
            posicaoAtual = obterPosicaoDestino(ordem);
        }

        return total;
    }

    public Map<String, Object> obterEstatisticasOtimizacao(List<OrdemTrabalhoPatio> ordensOriginais,
                                                           List<OrdemTrabalhoPatio> ordensOtimizadas) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrdens", ordensOriginais.size());
        stats.put("distanciaOriginal", calcularDistanciaTotal(ordensOriginais));
        stats.put("distanciaOtimizada", calcularDistanciaTotal(ordensOtimizadas));

        double mejora = stats.get("distanciaOriginal") != null && stats.get("distanciaOtimizada") != null
                ? ((Double) stats.get("distanciaOriginal") - (Double) stats.get("distanciaOtimizada"))
                / (Double) stats.get("distanciaOriginal") * 100
                : 0;
        stats.put("percentualMejora", mejora);

        return stats;
    }
}
