package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.util.YardDistanceCalculator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MapaOcupacaoServico {

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;

    public MapaOcupacaoServico(ConteinerPatioRepositorio conteinerRepositorio,
                                PosicaoPatioRepositorio posicaoRepositorio) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
    }

    @Transactional(readOnly = true)
    public HeatmapOcupacaoDto gerarHeatmap() {
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<PosicaoPatio> posicoes = posicaoRepositorio.findAll();

        int maxLinha = posicoes.stream()
                .mapToInt(PosicaoPatio::getLinha)
                .max()
                .orElse(100);
        int maxColuna = posicoes.stream()
                .mapToInt(PosicaoPatio::getColuna)
                .max()
                .orElse(100);

        double[][] heatmap = criarMatrizHeatmap(maxLinha + 1, maxColuna + 1);
        preencherHeatmap(heatmap, conteineres);
        List<ZonaOcupacaoDto> zonasAlta = identificarZonasAlta(heatmap);
        List<ZonaOcupacaoDto> zonasMedia = identificarZonasMedia(heatmap);
        List<ZonaOcupacaoDto> zonasBaixa = identificarZonasBaixa(heatmap);

        double percentualOcupacaoGeral = calcularPercentualOcupacao(conteineres, posicoes);

        return new HeatmapOcupacaoDto(
                heatmap,
                maxLinha + 1,
                maxColuna + 1,
                zonasAlta,
                zonasMedia,
                zonasBaixa,
                percentualOcupacaoGeral,
                identificarRotasEscape(conteineres)
        );
    }

    @Transactional(readOnly = true)
    public NivelOcupacaoEnum obterNivelOcupacao() {
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<PosicaoPatio> posicoes = posicaoRepositorio.findAll();

        double percentual = calcularPercentualOcupacao(conteineres, posicoes);

        if (percentual >= 85) {
            return NivelOcupacaoEnum.CRÍTICA;
        } else if (percentual >= 70) {
            return NivelOcupacaoEnum.ALTA;
        } else if (percentual >= 50) {
            return NivelOcupacaoEnum.MÉDIA;
        } else {
            return NivelOcupacaoEnum.BAIXA;
        }
    }

    @Transactional(readOnly = true)
    public Integer obterDistanciaMediaParaGate(ConteinerPatio conteiner) {
        if (conteiner == null || conteiner.getPosicao() == null) {
            return Integer.MAX_VALUE;
        }

        PosicaoPatio pos = conteiner.getPosicao();
        int linhaGate = 0;
        int colunaGate = 0;

        return YardDistanceCalculator.manhattan(pos.getLinha(), pos.getColuna(), linhaGate, colunaGate);
    }

    private double[][] criarMatrizHeatmap(int linhas, int colunas) {
        return new double[linhas][colunas];
    }

    private void preencherHeatmap(double[][] heatmap, List<ConteinerPatio> conteineres) {
        for (ConteinerPatio conteiner : conteineres) {
            if (conteiner.getPosicao() != null) {
                int linha = conteiner.getPosicao().getLinha();
                int coluna = conteiner.getPosicao().getColuna();

                if (linha < heatmap.length && coluna < heatmap[0].length) {
                    heatmap[linha][coluna] += 1;
                }
            }
        }
    }

    private List<ZonaOcupacaoDto> identificarZonasAlta(double[][] heatmap) {
        return identificarZonas(heatmap, 7, 10, "ALTA");
    }

    private List<ZonaOcupacaoDto> identificarZonasMedia(double[][] heatmap) {
        return identificarZonas(heatmap, 4, 6, "MÉDIA");
    }

    private List<ZonaOcupacaoDto> identificarZonasBaixa(double[][] heatmap) {
        return identificarZonas(heatmap, 1, 3, "BAIXA");
    }

    private List<ZonaOcupacaoDto> identificarZonas(double[][] heatmap, int minOcupacao,
                                                     int maxOcupacao, String nivel) {
        List<ZonaOcupacaoDto> zonas = new java.util.ArrayList<>();

        for (int i = 0; i < heatmap.length; i++) {
            for (int j = 0; j < heatmap[0].length; j++) {
                int ocupacao = (int) heatmap[i][j];
                if (ocupacao >= minOcupacao && ocupacao <= maxOcupacao) {
                    zonas.add(new ZonaOcupacaoDto(i, j, ocupacao, nivel));
                }
            }
        }

        return zonas;
    }

    private List<RotaEscapeDto> identificarRotasEscape(List<ConteinerPatio> conteineres) {
        List<RotaEscapeDto> rotas = new java.util.ArrayList<>();

        conteineres.stream()
                .filter(c -> c.getPosicao() != null)
                .filter(c -> obterDistanciaMediaParaGate(c) < 20)
                .forEach(c -> rotas.add(
                        new RotaEscapeDto(
                                c.getCodigo(),
                                c.getPosicao().getLinha(),
                                c.getPosicao().getColuna(),
                                obterDistanciaMediaParaGate(c),
                                "PRIORITÁRIA"
                        )
                ));

        return rotas;
    }

    private double calcularPercentualOcupacao(List<ConteinerPatio> conteineres,
                                               List<PosicaoPatio> posicoes) {
        if (posicoes.isEmpty()) {
            return 0;
        }
        return (double) conteineres.size() / posicoes.size() * 100;
    }

    public enum NivelOcupacaoEnum {
        CRÍTICA("Ocupação acima de 85% - Ativar priorização total"),
        ALTA("Ocupação entre 70-85% - Priorizar zonas de manobra"),
        MÉDIA("Ocupação entre 50-70% - Operação normal"),
        BAIXA("Ocupação abaixo de 50% - Ideal para re-shuffling");

        private final String descricao;

        NivelOcupacaoEnum(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public static class HeatmapOcupacaoDto {
        private double[][] matriz;
        private int totalLinhas;
        private int totalColunas;
        private List<ZonaOcupacaoDto> zonasAlta;
        private List<ZonaOcupacaoDto> zonasMedia;
        private List<ZonaOcupacaoDto> zonasBaixa;
        private double percentualOcupacaoGeral;
        private List<RotaEscapeDto> rotasEscape;

        public HeatmapOcupacaoDto(double[][] matriz, int totalLinhas, int totalColunas,
                                  List<ZonaOcupacaoDto> zonasAlta, List<ZonaOcupacaoDto> zonasMedia,
                                  List<ZonaOcupacaoDto> zonasBaixa, double percentualOcupacaoGeral,
                                  List<RotaEscapeDto> rotasEscape) {
            this.matriz = matriz;
            this.totalLinhas = totalLinhas;
            this.totalColunas = totalColunas;
            this.zonasAlta = zonasAlta;
            this.zonasMedia = zonasMedia;
            this.zonasBaixa = zonasBaixa;
            this.percentualOcupacaoGeral = percentualOcupacaoGeral;
            this.rotasEscape = rotasEscape;
        }

        public double[][] getMatriz() { return matriz; }
        public int getTotalLinhas() { return totalLinhas; }
        public int getTotalColunas() { return totalColunas; }
        public List<ZonaOcupacaoDto> getZonasAlta() { return zonasAlta; }
        public List<ZonaOcupacaoDto> getZonasMedia() { return zonasMedia; }
        public List<ZonaOcupacaoDto> getZonasBaixa() { return zonasBaixa; }
        public double getPercentualOcupacaoGeral() { return percentualOcupacaoGeral; }
        public List<RotaEscapeDto> getRotasEscape() { return rotasEscape; }
    }

    public static class ZonaOcupacaoDto {
        private int linha;
        private int coluna;
        private int ocupacao;
        private String nivel;

        public ZonaOcupacaoDto(int linha, int coluna, int ocupacao, String nivel) {
            this.linha = linha;
            this.coluna = coluna;
            this.ocupacao = ocupacao;
            this.nivel = nivel;
        }

        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
        public int getOcupacao() { return ocupacao; }
        public String getNivel() { return nivel; }
    }

    public static class RotaEscapeDto {
        private String codigoConteiner;
        private int linhaAtual;
        private int colunaAtual;
        private int distanciaParaGate;
        private String prioridade;

        public RotaEscapeDto(String codigoConteiner, int linhaAtual, int colunaAtual,
                            int distanciaParaGate, String prioridade) {
            this.codigoConteiner = codigoConteiner;
            this.linhaAtual = linhaAtual;
            this.colunaAtual = colunaAtual;
            this.distanciaParaGate = distanciaParaGate;
            this.prioridade = prioridade;
        }

        public String getCodigoConteiner() { return codigoConteiner; }
        public int getLinhaAtual() { return linhaAtual; }
        public int getColunaAtual() { return colunaAtual; }
        public int getDistanciaParaGate() { return distanciaParaGate; }
        public String getPrioridade() { return prioridade; }
    }
}
