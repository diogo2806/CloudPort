package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtimizadorRotasPatioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;

    public OtimizadorRotasPatioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio) {
        this.ordemRepositorio = ordemRepositorio;
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
        return aplicarDualCyclingBasico(otimizadas);
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

    private List<OrdemTrabalhoPatio> aplicarDualCyclingBasico(List<OrdemTrabalhoPatio> ordens) {
        List<OrdemTrabalhoPatio> resultado = new ArrayList<>();
        Map<String, List<OrdemTrabalhoPatio>> porDestino = agruparPorDestino(ordens);

        for (List<OrdemTrabalhoPatio> grupo : porDestino.values()) {
            grupo.sort(Comparator.comparing(o -> calcularDistancia(
                    obterPosicaoAtual(o),
                    obterPosicaoDestino(o)
            )));
            resultado.addAll(grupo);
        }

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
        int deltaLinha = p1.getLinha() - p2.getLinha();
        int deltaColuna = p1.getColuna() - p2.getColuna();
        return Math.sqrt(deltaLinha * deltaLinha + deltaColuna * deltaColuna);
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

    private Map<String, List<OrdemTrabalhoPatio>> agruparPorDestino(List<OrdemTrabalhoPatio> ordens) {
        Map<String, List<OrdemTrabalhoPatio>> grupos = new HashMap<>();
        for (OrdemTrabalhoPatio ordem : ordens) {
            grupos.computeIfAbsent(ordem.getDestino(), k -> new ArrayList<>()).add(ordem);
        }
        return grupos;
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
