package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GerenciadorZonasBufferServico {

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private Set<String> zonasBufferReservadas = new HashSet<>();

    public GerenciadorZonasBufferServico(ConteinerPatioRepositorio conteinerRepositorio,
                                          PosicaoPatioRepositorio posicaoRepositorio) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        inicializarZonasBuffer();
    }

    @Transactional(readOnly = true)
    public ConfiguracaoBufferDto obterConfiguracaoBuffer() {
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<PosicaoPatio> todasPosicoes = posicaoRepositorio.findAll();

        int maxLinha = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getLinha)
                .max()
                .orElse(100);
        int maxColuna = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getColuna)
                .max()
                .orElse(100);

        List<ZonaBufferDto> zonas = identificarZonasBuffer(maxLinha, maxColuna);
        double percentualOcupacaoZonas = calcularOcupacaoZonasBuffer(conteineres);
        List<String> corredoresLivres = identificarCorredoresLivres(conteineres, maxLinha, maxColuna);

        return new ConfiguracaoBufferDto(
                zonas,
                corredoresLivres,
                percentualOcupacaoZonas,
                zonasBufferReservadas.size()
        );
    }

    @Transactional(readOnly = true)
    public boolean podeAlocarEmZona(Integer linha, Integer coluna) {
        String zonaChave = extrairZonaChave(linha, coluna);
        return !zonasBufferReservadas.contains(zonaChave);
    }

    @Transactional
    public void reservarZonaBuffer(Integer linha, Integer coluna, String motivo) {
        String zonaChave = extrairZonaChave(linha, coluna);
        zonasBufferReservadas.add(zonaChave);
    }

    @Transactional
    public void liberarZonaBuffer(Integer linha, Integer coluna) {
        String zonaChave = extrairZonaChave(linha, coluna);
        zonasBufferReservadas.remove(zonaChave);
    }

    @Transactional(readOnly = true)
    public AnaliseCorredoresDto analisarCorredoresManobra() {
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<PosicaoPatio> todasPosicoes = posicaoRepositorio.findAll();

        int maxLinha = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getLinha)
                .max()
                .orElse(100);
        int maxColuna = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getColuna)
                .max()
                .orElse(100);

        List<CorredorManobraDto> corredores = identificarCorredoresManobra(conteineres, maxLinha, maxColuna);
        double larguraMediaCorredor = corredores.stream()
                .mapToDouble(CorredorManobraDto::getLargura)
                .average()
                .orElse(0);

        return new AnaliseCorredoresDto(
                corredores,
                larguraMediaCorredor,
                corredores.stream().filter(c -> c.getLargura() < 3).count(),
                calcularTamanhoTotalCorredor(corredores)
        );
    }

    @Transactional(readOnly = true)
    public List<ZonaAlertaDto> identificarZonasEmRisco() {
        List<ZonaAlertaDto> alertas = new ArrayList<>();
        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<PosicaoPatio> todasPosicoes = posicaoRepositorio.findAll();

        int maxLinha = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getLinha)
                .max()
                .orElse(100);
        int maxColuna = todasPosicoes.stream()
                .mapToInt(PosicaoPatio::getColuna)
                .max()
                .orElse(100);

        for (int i = 0; i <= maxLinha; i += 20) {
            for (int j = 0; j <= maxColuna; j += 20) {
                long conteineresNaZona = conteineres.stream()
                        .filter(c -> c.getPosicao() != null)
                        .filter(c -> c.getPosicao().getLinha() >= i && c.getPosicao().getLinha() < i + 20)
                        .filter(c -> c.getPosicao().getColuna() >= j && c.getPosicao().getColuna() < j + 20)
                        .count();

                double percentualOcupacao = (conteineresNaZona * 100.0) / 400;

                if (percentualOcupacao >= 80) {
                    alertas.add(new ZonaAlertaDto(
                            i, j,
                            (int) conteineresNaZona,
                            percentualOcupacao,
                            "CRÍTICA",
                            "Zona congestionada - impedir alocação adicional"
                    ));
                } else if (percentualOcupacao >= 60) {
                    alertas.add(new ZonaAlertaDto(
                            i, j,
                            (int) conteineresNaZona,
                            percentualOcupacao,
                            "ALERTA",
                            "Zona em aquecimento - monitorar ocupação"
                    ));
                }
            }
        }

        return alertas;
    }

    private void inicializarZonasBuffer() {
        // Reservar corredores de passagem no centro do pátio (linhas e colunas estratégicas)
        // Formato: "BLOCO_[linha]_[coluna]"
        zonasBufferReservadas.add("BLOCO_5_*");  // Corredor horizontal no meio
        zonasBufferReservadas.add("BLOCO_*_5");  // Corredor vertical no meio
    }

    private List<ZonaBufferDto> identificarZonasBuffer(int maxLinha, int maxColuna) {
        List<ZonaBufferDto> zonas = new ArrayList<>();

        // Corredores horizontais (linhas estratégicas)
        for (int i = 5; i <= maxLinha; i += 10) {
            for (int j = 0; j <= maxColuna; j++) {
                zonas.add(new ZonaBufferDto(i, j, "HORIZONTAL", "Corredor de passagem"));
            }
        }

        // Corredores verticais (colunas estratégicas)
        for (int i = 0; i <= maxLinha; i++) {
            for (int j = 5; j <= maxColuna; j += 10) {
                zonas.add(new ZonaBufferDto(i, j, "VERTICAL", "Corredor de passagem"));
            }
        }

        return zonas;
    }

    private List<String> identificarCorredoresLivres(List<ConteinerPatio> conteineres,
                                                      int maxLinha, int maxColuna) {
        List<String> corredoresLivres = new ArrayList<>();

        // Verificar linhas horizontais
        for (int i = 5; i <= maxLinha; i += 10) {
            long ocupacao = conteineres.stream()
                    .filter(c -> c.getPosicao() != null)
                    .filter(c -> c.getPosicao().getLinha() == i)
                    .count();

            if (ocupacao == 0) {
                corredoresLivres.add("HORIZONTAL_" + i);
            }
        }

        // Verificar colunas verticais
        for (int j = 5; j <= maxColuna; j += 10) {
            long ocupacao = conteineres.stream()
                    .filter(c -> c.getPosicao() != null)
                    .filter(c -> c.getPosicao().getColuna() == j)
                    .count();

            if (ocupacao == 0) {
                corredoresLivres.add("VERTICAL_" + j);
            }
        }

        return corredoresLivres;
    }

    private List<CorredorManobraDto> identificarCorredoresManobra(List<ConteinerPatio> conteineres,
                                                                   int maxLinha, int maxColuna) {
        List<CorredorManobraDto> corredores = new ArrayList<>();

        // Corredores principais (cada 10 posições)
        for (int i = 5; i <= maxLinha; i += 10) {
            long largura = conteineres.stream()
                    .filter(c -> c.getPosicao() != null)
                    .filter(c -> c.getPosicao().getLinha() >= i - 2 && c.getPosicao().getLinha() <= i + 2)
                    .count();

            corredores.add(new CorredorManobraDto(
                    "HORIZONTAL_" + i,
                    i,
                    (int) (10 - largura),
                    "Corredor horizontal principal"
            ));
        }

        return corredores;
    }

    private double calcularOcupacaoZonasBuffer(List<ConteinerPatio> conteineres) {
        long conteineresBuffer = conteineres.stream()
                .filter(c -> c.getPosicao() != null)
                .filter(c -> c.getPosicao().getLinha() == 5 || c.getPosicao().getColuna() == 5)
                .count();

        return (conteineresBuffer * 100.0) / (conteineres.size() > 0 ? conteineres.size() : 1);
    }

    private double calcularTamanhoTotalCorredor(List<CorredorManobraDto> corredores) {
        return corredores.stream()
                .mapToDouble(CorredorManobraDto::getLargura)
                .sum();
    }

    private String extrairZonaChave(Integer linha, Integer coluna) {
        int blocoLinha = (linha != null ? linha : 0) / 10;
        int blocoColuna = (coluna != null ? coluna : 0) / 10;
        return "BLOCO_" + blocoLinha + "_" + blocoColuna;
    }

    public static class ConfiguracaoBufferDto {
        private List<ZonaBufferDto> zonas;
        private List<String> corredoresLivres;
        private double percentualOcupacaoZonas;
        private int totalZonasReservadas;

        public ConfiguracaoBufferDto(List<ZonaBufferDto> zonas,
                                      List<String> corredoresLivres,
                                      double percentualOcupacaoZonas,
                                      int totalZonasReservadas) {
            this.zonas = zonas;
            this.corredoresLivres = corredoresLivres;
            this.percentualOcupacaoZonas = percentualOcupacaoZonas;
            this.totalZonasReservadas = totalZonasReservadas;
        }

        public List<ZonaBufferDto> getZonas() { return zonas; }
        public List<String> getCorredoresLivres() { return corredoresLivres; }
        public double getPercentualOcupacaoZonas() { return percentualOcupacaoZonas; }
        public int getTotalZonasReservadas() { return totalZonasReservadas; }
    }

    public static class ZonaBufferDto {
        private int linha;
        private int coluna;
        private String tipo;
        private String descricao;

        public ZonaBufferDto(int linha, int coluna, String tipo, String descricao) {
            this.linha = linha;
            this.coluna = coluna;
            this.tipo = tipo;
            this.descricao = descricao;
        }

        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
        public String getTipo() { return tipo; }
        public String getDescricao() { return descricao; }
    }

    public static class CorredorManobraDto {
        private String identificador;
        private int posicao;
        private int largura;
        private String descricao;

        public CorredorManobraDto(String identificador, int posicao, int largura, String descricao) {
            this.identificador = identificador;
            this.posicao = posicao;
            this.largura = largura;
            this.descricao = descricao;
        }

        public String getIdentificador() { return identificador; }
        public int getPosicao() { return posicao; }
        public int getLargura() { return largura; }
        public String getDescricao() { return descricao; }
    }

    public static class AnaliseCorredoresDto {
        private List<CorredorManobraDto> corredores;
        private double larguraMediaCorredor;
        private long corredoresEmRisco;
        private double tamanhoTotalCorredor;

        public AnaliseCorredoresDto(List<CorredorManobraDto> corredores,
                                     double larguraMediaCorredor,
                                     long corredoresEmRisco,
                                     double tamanhoTotalCorredor) {
            this.corredores = corredores;
            this.larguraMediaCorredor = larguraMediaCorredor;
            this.corredoresEmRisco = corredoresEmRisco;
            this.tamanhoTotalCorredor = tamanhoTotalCorredor;
        }

        public List<CorredorManobraDto> getCorredores() { return corredores; }
        public double getLarguraMediaCorredor() { return larguraMediaCorredor; }
        public long getCorredoresEmRisco() { return corredoresEmRisco; }
        public double getTamanhoTotalCorredor() { return tamanhoTotalCorredor; }
    }

    public static class ZonaAlertaDto {
        private int linha;
        private int coluna;
        private int conteineresPresentes;
        private double percentualOcupacao;
        private String nivelAlerta;
        private String recomendacao;

        public ZonaAlertaDto(int linha, int coluna, int conteineresPresentes,
                            double percentualOcupacao, String nivelAlerta, String recomendacao) {
            this.linha = linha;
            this.coluna = coluna;
            this.conteineresPresentes = conteineresPresentes;
            this.percentualOcupacao = percentualOcupacao;
            this.nivelAlerta = nivelAlerta;
            this.recomendacao = recomendacao;
        }

        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
        public int getConteineresPresentes() { return conteineresPresentes; }
        public double getPercentualOcupacao() { return percentualOcupacao; }
        public String getNivelAlerta() { return nivelAlerta; }
        public String getRecomendacao() { return recomendacao; }
    }
}
