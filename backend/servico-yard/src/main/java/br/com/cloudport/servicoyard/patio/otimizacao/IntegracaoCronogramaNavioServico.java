package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegracaoCronogramaNavioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;

    public IntegracaoCronogramaNavioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                             EquipamentoPatioRepositorio equipamentoRepositorio) {
        this.ordemRepositorio = ordemRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    @Transactional(readOnly = true)
    public PriorizacaoRtgPorNavioDto calcularPriorizacaoRtgPorNavio(LocalDateTime dataPartidaNavio) {
        List<EquipamentoPatio> rtgsOperacionais = equipamentoRepositorio
                .findByTipoEquipamentoAndStatusOperacional(
                        TipoEquipamento.RTG,
                        StatusEquipamento.OPERACIONAL
                );

        long tempoMinutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), dataPartidaNavio);

        List<PriorizacaoRtgDto> priorizacoes = rtgsOperacionais.stream()
                .map(rtg -> calcularPriorizacaoRtg(rtg, tempoMinutosRestantes))
                .sorted(Comparator.comparing(PriorizacaoRtgDto::getNivelPrioridade).reversed())
                .collect(Collectors.toList());

        return new PriorizacaoRtgPorNavioDto(
                dataPartidaNavio,
                tempoMinutosRestantes,
                priorizacoes,
                definirNivelUrgencia(tempoMinutosRestantes)
        );
    }

    @Transactional(readOnly = true)
    public List<SequenciaOperacaoRtgPorNavioDto> obterSequenciaOtimizadaPorNavio(
            LocalDateTime dataPartidaNavio) {

        List<EquipamentoPatio> rtgsOperacionais = equipamentoRepositorio
                .findByTipoEquipamentoAndStatusOperacional(
                        TipoEquipamento.RTG,
                        StatusEquipamento.OPERACIONAL
                );

        long tempoMinutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), dataPartidaNavio);

        return rtgsOperacionais.stream()
                .map(rtg -> construirSequenciaRtg(rtg, tempoMinutosRestantes))
                .sorted(Comparator.comparing(SequenciaOperacaoRtgPorNavioDto::getPrioridade).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnaliseCapacidadeNavioDto analisarCapacidadeParaNavio(LocalDateTime dataPartidaNavio) {
        List<OrdemTrabalhoPatio> ordensRestantes = ordemRepositorio.findAll().stream()
                .filter(o -> o.getConcluidoEm() == null)
                .collect(Collectors.toList());

        long ordensEntrada = ordensRestantes.stream()
                .filter(o -> o.getTipoMovimento().toString().equals("ALOCACAO"))
                .count();

        long ordensSaida = ordensRestantes.stream()
                .filter(o -> o.getTipoMovimento().toString().equals("REMOCAO"))
                .count();

        long tempoMinutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), dataPartidaNavio);
        double tempoHoras = tempoMinutosRestantes / 60.0;

        int rtgsDisponiveis = (int) equipamentoRepositorio
                .findByTipoEquipamentoAndStatusOperacional(
                        TipoEquipamento.RTG,
                        StatusEquipamento.OPERACIONAL
                ).stream().count();

        double capacidadeHora = rtgsDisponiveis * 2.5;
        double ordensPorHora = (ordensEntrada + ordensSaida) / Math.max(tempoHoras, 1);
        double percentualCapacidade = (ordensPorHora / capacidadeHora) * 100;

        return new AnaliseCapacidadeNavioDto(
                dataPartidaNavio,
                ordensEntrada,
                ordensSaida,
                tempoMinutosRestantes,
                rtgsDisponiveis,
                capacidadeHora,
                ordensPorHora,
                percentualCapacidade,
                classificarRisco(percentualCapacidade)
        );
    }

    @Transactional(readOnly = true)
    public List<AlertaOperacionalDto> identificarAlertasOperacionais(LocalDateTime dataPartidaNavio) {
        List<AlertaOperacionalDto> alertas = new ArrayList<>();

        AnaliseCapacidadeNavioDto analise = analisarCapacidadeParaNavio(dataPartidaNavio);

        if (analise.getPercentualCapacidade() > 100) {
            alertas.add(new AlertaOperacionalDto(
                    "CAPACIDADE_INSUFICIENTE",
                    "CRÍTICA",
                    "RTGs insuficientes para atender cronograma",
                    "Aumentar quantidade de RTGs ou solicitar adiamento"
            ));
        } else if (analise.getPercentualCapacidade() > 80) {
            alertas.add(new AlertaOperacionalDto(
                    "CAPACIDADE_MARGEM_BAIXA",
                    "ALERTA",
                    "Operação próxima do limite de capacidade",
                    "Monitorar intensamente e considerar ações preventivas"
            ));
        }

        if (analise.getTempoMinutosRestantes() < 120) {
            alertas.add(new AlertaOperacionalDto(
                    "TEMPO_CRITICO",
                    "CRÍTICA",
                    "Menos de 2 horas para partida do navio",
                    "Escalar para supervisor - ativar plano de emergência"
            ));
        } else if (analise.getTempoMinutosRestantes() < 480) {
            alertas.add(new AlertaOperacionalDto(
                    "TEMPO_CRITICO",
                    "ALERTA",
                    "Menos de 8 horas para partida do navio",
                    "Aumentar ritmo de operações - evitar atrasos"
            ));
        }

        List<EquipamentoPatio> rtgsOciosos = equipamentoRepositorio
                .findByTipoEquipamentoAndStatusOperacional(
                        TipoEquipamento.RTG,
                        StatusEquipamento.OPERACIONAL
                ).stream()
                .filter(rtg -> verificarRtgOcioso(rtg))
                .collect(Collectors.toList());

        if (!rtgsOciosos.isEmpty()) {
            alertas.add(new AlertaOperacionalDto(
                    "RTG_OCIOSO",
                    "AVISO",
                    rtgsOciosos.size() + " RTGs sem atribuição",
                    "Distribuir ordens pendentes para RTGs ociosos"
            ));
        }

        return alertas;
    }

    private PriorizacaoRtgDto calcularPriorizacaoRtg(EquipamentoPatio rtg, long tempoMinutosRestantes) {
        int prioridade = calcularPriorizacaoBase(tempoMinutosRestantes);

        // Aumentar prioridade se RTG está em posição estratégica
        if (rtg.getLinha() != null && rtg.getLinha() < 20) {
            prioridade += 20;
        }

        // Diminuir se RTG está longe
        if (rtg.getLinha() != null && rtg.getLinha() > 80) {
            prioridade -= 10;
        }

        return new PriorizacaoRtgDto(
                rtg.getIdentificador(),
                prioridade,
                rtg.getLinha(),
                rtg.getColuna(),
                classificarNivelPrioridade(prioridade)
        );
    }

    private SequenciaOperacaoRtgPorNavioDto construirSequenciaRtg(EquipamentoPatio rtg,
                                                                   long tempoMinutosRestantes) {
        int prioridade = calcularPriorizacaoBase(tempoMinutosRestantes);

        if (rtg.getLinha() != null && rtg.getLinha() < 20) {
            prioridade += 20;
        }

        return new SequenciaOperacaoRtgPorNavioDto(
                rtg.getIdentificador(),
                prioridade,
                rtg.getLinha(),
                rtg.getColuna(),
                (int) (tempoMinutosRestantes / 60),
                classificarNivelPrioridade(prioridade)
        );
    }

    private int calcularPriorizacaoBase(long tempoMinutosRestantes) {
        if (tempoMinutosRestantes < 120) {
            return 100;
        } else if (tempoMinutosRestantes < 480) {
            return 80;
        } else if (tempoMinutosRestantes < 1440) {
            return 60;
        } else {
            return 40;
        }
    }

    private String classificarNivelPrioridade(int prioridade) {
        if (prioridade >= 100) {
            return "CRÍTICA";
        } else if (prioridade >= 80) {
            return "ALTA";
        } else if (prioridade >= 60) {
            return "MÉDIA";
        } else {
            return "BAIXA";
        }
    }

    private String definirNivelUrgencia(long tempoMinutosRestantes) {
        if (tempoMinutosRestantes < 120) {
            return "EMERGÊNCIA";
        } else if (tempoMinutosRestantes < 480) {
            return "CRÍTICA";
        } else if (tempoMinutosRestantes < 1440) {
            return "ALTA";
        } else {
            return "NORMAL";
        }
    }

    private String classificarRisco(double percentualCapacidade) {
        if (percentualCapacidade > 100) {
            return "CRÍTICO";
        } else if (percentualCapacidade > 80) {
            return "ALTO";
        } else if (percentualCapacidade > 60) {
            return "MÉDIO";
        } else {
            return "BAIXO";
        }
    }

    private boolean verificarRtgOcioso(EquipamentoPatio rtg) {
        List<OrdemTrabalhoPatio> ordensParaRtg = ordemRepositorio.findAll().stream()
                .filter(o -> o.getColunaDestino() != null && o.getColunaDestino().equals(rtg.getColuna()))
                .filter(o -> o.getConcluidoEm() == null)
                .collect(Collectors.toList());

        return ordensParaRtg.isEmpty();
    }

    public static class PriorizacaoRtgPorNavioDto {
        private LocalDateTime dataPartidaNavio;
        private long tempoMinutosRestantes;
        private List<PriorizacaoRtgDto> priorizacoes;
        private String nivelUrgencia;

        public PriorizacaoRtgPorNavioDto(LocalDateTime dataPartidaNavio,
                                         long tempoMinutosRestantes,
                                         List<PriorizacaoRtgDto> priorizacoes,
                                         String nivelUrgencia) {
            this.dataPartidaNavio = dataPartidaNavio;
            this.tempoMinutosRestantes = tempoMinutosRestantes;
            this.priorizacoes = priorizacoes;
            this.nivelUrgencia = nivelUrgencia;
        }

        public LocalDateTime getDataPartidaNavio() { return dataPartidaNavio; }
        public long getTempoMinutosRestantes() { return tempoMinutosRestantes; }
        public List<PriorizacaoRtgDto> getPriorizacoes() { return priorizacoes; }
        public String getNivelUrgencia() { return nivelUrgencia; }
    }

    public static class PriorizacaoRtgDto {
        private String identificadorRtg;
        private int prioridade;
        private Integer linha;
        private Integer coluna;
        private String nivelPrioridade;

        public PriorizacaoRtgDto(String identificadorRtg, int prioridade,
                                 Integer linha, Integer coluna, String nivelPrioridade) {
            this.identificadorRtg = identificadorRtg;
            this.prioridade = prioridade;
            this.linha = linha;
            this.coluna = coluna;
            this.nivelPrioridade = nivelPrioridade;
        }

        public String getIdentificadorRtg() { return identificadorRtg; }
        public int getPrioridade() { return prioridade; }
        public Integer getLinha() { return linha; }
        public Integer getColuna() { return coluna; }
        public String getNivelPrioridade() { return nivelPrioridade; }
    }

    public static class SequenciaOperacaoRtgPorNavioDto {
        private String identificadorRtg;
        private int prioridade;
        private Integer linha;
        private Integer coluna;
        private int tempoHorasDisponiveis;
        private String nivelPrioridade;

        public SequenciaOperacaoRtgPorNavioDto(String identificadorRtg, int prioridade,
                                               Integer linha, Integer coluna,
                                               int tempoHorasDisponiveis, String nivelPrioridade) {
            this.identificadorRtg = identificadorRtg;
            this.prioridade = prioridade;
            this.linha = linha;
            this.coluna = coluna;
            this.tempoHorasDisponiveis = tempoHorasDisponiveis;
            this.nivelPrioridade = nivelPrioridade;
        }

        public String getIdentificadorRtg() { return identificadorRtg; }
        public int getPrioridade() { return prioridade; }
        public Integer getLinha() { return linha; }
        public Integer getColuna() { return coluna; }
        public int getTempoHorasDisponiveis() { return tempoHorasDisponiveis; }
        public String getNivelPrioridade() { return nivelPrioridade; }
    }

    public static class AnaliseCapacidadeNavioDto {
        private LocalDateTime dataPartidaNavio;
        private long ordensEntrada;
        private long ordensSaida;
        private long tempoMinutosRestantes;
        private int rtgsDisponiveis;
        private double capacidadeHora;
        private double ordensPorHora;
        private double percentualCapacidade;
        private String classificacaoRisco;

        public AnaliseCapacidadeNavioDto(LocalDateTime dataPartidaNavio,
                                         long ordensEntrada,
                                         long ordensSaida,
                                         long tempoMinutosRestantes,
                                         int rtgsDisponiveis,
                                         double capacidadeHora,
                                         double ordensPorHora,
                                         double percentualCapacidade,
                                         String classificacaoRisco) {
            this.dataPartidaNavio = dataPartidaNavio;
            this.ordensEntrada = ordensEntrada;
            this.ordensSaida = ordensSaida;
            this.tempoMinutosRestantes = tempoMinutosRestantes;
            this.rtgsDisponiveis = rtgsDisponiveis;
            this.capacidadeHora = capacidadeHora;
            this.ordensPorHora = ordensPorHora;
            this.percentualCapacidade = percentualCapacidade;
            this.classificacaoRisco = classificacaoRisco;
        }

        public LocalDateTime getDataPartidaNavio() { return dataPartidaNavio; }
        public long getOrdensEntrada() { return ordensEntrada; }
        public long getOrdensSaida() { return ordensSaida; }
        public long getTempoMinutosRestantes() { return tempoMinutosRestantes; }
        public int getRtgsDisponiveis() { return rtgsDisponiveis; }
        public double getCapacidadeHora() { return capacidadeHora; }
        public double getOrdensPorHora() { return ordensPorHora; }
        public double getPercentualCapacidade() { return percentualCapacidade; }
        public String getClassificacaoRisco() { return classificacaoRisco; }
    }

    public static class AlertaOperacionalDto {
        private String tipo;
        private String severidade;
        private String descricao;
        private String recomendacao;

        public AlertaOperacionalDto(String tipo, String severidade, String descricao, String recomendacao) {
            this.tipo = tipo;
            this.severidade = severidade;
            this.descricao = descricao;
            this.recomendacao = recomendacao;
        }

        public String getTipo() { return tipo; }
        public String getSeveridade() { return severidade; }
        public String getDescricao() { return descricao; }
        public String getRecomendacao() { return recomendacao; }
    }
}
