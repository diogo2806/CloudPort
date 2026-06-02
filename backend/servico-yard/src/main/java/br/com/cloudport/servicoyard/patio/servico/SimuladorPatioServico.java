package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.CenarioSimulacaoDto;
import br.com.cloudport.servicoyard.patio.dto.ResultadoSimulacaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimuladorPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final AlertasPatioServico alertasPatioServico;

    public SimuladorPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                                  EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                                  AlertasPatioServico alertasPatioServico) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.alertasPatioServico = alertasPatioServico;
    }

    @Transactional(readOnly = true)
    public ResultadoSimulacaoDto simularCenario(CenarioSimulacaoDto cenario) {
        List<ConteinerPatio> conteineresCurrent = conteinerPatioRepositorio.findAll();
        List<EquipamentoPatio> equipamentosCurrent = equipamentoPatioRepositorio.findAll();

        var metricasAtuais = calcularMetricas(conteineresCurrent, equipamentosCurrent);

        var conteinereSimulado = simularChanges(conteineresCurrent, equipamentosCurrent, cenario);
        var metricsSimulado = calcularMetricas(conteinereSimulado.conteineres(), conteinereSimulado.equipamentos());

        String alertaPrincipal = gerarAlertaPrincipal(cenario, metricsSimulado);
        String impacto = gerarImpactoProducao(cenario, metricasAtuais, metricsSimulado);
        String recomendacao = gerarRecomendacao(cenario, metricsSimulado);

        return new ResultadoSimulacaoDto(
            cenario.getDescricao(),
            metricasAtuais.totalConteineres(),
            metricasAtuais.ocupacao(),
            metricasAtuais.rehandleRatio(),
            metricasAtuais.equipamentosDisponiveis(),
            metricsSimulado.totalConteineres(),
            metricsSimulado.ocupacao(),
            metricsSimulado.rehandleRatio(),
            metricsSimulado.equipamentosDisponiveis(),
            alertaPrincipal,
            recomendacao,
            impacto
        );
    }

    private record MetricasPatio(Integer totalConteineres, Integer ocupacao, Double rehandleRatio, Integer equipamentosDisponiveis) {}

    private MetricasPatio calcularMetricas(List<ConteinerPatio> conteineres, List<EquipamentoPatio> equipamentos) {
        int totalConteineres = conteineres.size();
        int ocupacao = totalConteineres > 0 ? (totalConteineres * 100) / Math.max(1, totalConteineres + 20) : 0;

        double rehandleRatio = 0.0;
        if (conteineres.size() > 0) {
            int totalRehandles = conteineres.stream()
                .mapToInt(c -> calcularRehandlesParaConteiner(c, conteineres))
                .sum();
            rehandleRatio = (totalRehandles * 100.0) / conteineres.size();
        }

        long equipamentosDisponiveis = equipamentos.stream()
            .filter(e -> e.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
            .count();

        return new MetricasPatio(totalConteineres, ocupacao, rehandleRatio, (int) equipamentosDisponiveis);
    }

    private int calcularRehandlesParaConteiner(ConteinerPatio conteiner, List<ConteinerPatio> todos) {
        int mesmaColuna = (int) todos.stream()
            .filter(c -> c.getPosicao().getColuna().equals(conteiner.getPosicao().getColuna()))
            .filter(c -> c.getPosicao().getLinha() < conteiner.getPosicao().getLinha())
            .count();
        return mesmaColuna;
    }

    private record SimulacaoResult(List<ConteinerPatio> conteineres, List<EquipamentoPatio> equipamentos) {}

    private SimulacaoResult simularChanges(List<ConteinerPatio> conteineres,
                                            List<EquipamentoPatio> equipamentos,
                                            CenarioSimulacaoDto cenario) {
        switch (cenario.getTipoCenario()) {
            case ATRASO_NAVIO:
                return simularAtrasoNavio(conteineres, equipamentos, cenario);
            case MANUTENCAO_EQUIPAMENTO:
                return simularManutencaoEquipamento(conteineres, equipamentos, cenario);
            case AUMENTO_VOLUME:
                return simularAumentoVolume(conteineres, equipamentos, cenario);
            default:
                return new SimulacaoResult(conteineres, equipamentos);
        }
    }

    private SimulacaoResult simularAtrasoNavio(List<ConteinerPatio> conteineres,
                                                List<EquipamentoPatio> equipamentos,
                                                CenarioSimulacaoDto cenario) {
        int horasAtraso = cenario.getHorasAtraso() != null ? cenario.getHorasAtraso() : 4;
        int conteineresPendentes = Math.max(50, conteineres.size() / 5);

        var conteineresCopy = conteineres.stream()
            .collect(Collectors.toList());

        for (int i = 0; i < conteineresPendentes && i < 30; i++) {
            if (i < conteineresCopy.size()) {
                conteineresCopy.remove(0);
            }
        }

        return new SimulacaoResult(conteineresCopy, equipamentos);
    }

    private SimulacaoResult simularManutencaoEquipamento(List<ConteinerPatio> conteineres,
                                                          List<EquipamentoPatio> equipamentos,
                                                          CenarioSimulacaoDto cenario) {
        var equipamentosCopy = equipamentos.stream()
            .collect(Collectors.toList());

        equipamentosCopy.stream()
            .filter(e -> e.getIdentificador().equals(cenario.getCodigoEquipamento()))
            .forEach(e -> e.setStatusOperacional(StatusEquipamento.MANUTENCAO));

        return new SimulacaoResult(conteineres, equipamentosCopy);
    }

    private SimulacaoResult simularAumentoVolume(List<ConteinerPatio> conteineres,
                                                  List<EquipamentoPatio> equipamentos,
                                                  CenarioSimulacaoDto cenario) {
        int quantidade = cenario.getQuantidadeConteinoresAdicionais() != null
            ? cenario.getQuantidadeConteinoresAdicionais()
            : 100;

        var conteineresCopy = conteineres.stream()
            .collect(Collectors.toList());

        for (int i = 0; i < quantidade; i++) {
            ConteinerPatio novoConteiner = new ConteinerPatio();
            novoConteiner.setCodigo("SIM-" + System.currentTimeMillis() + "-" + i);
            novoConteiner.setDestino("SIMULADO");
            conteineresCopy.add(novoConteiner);
        }

        return new SimulacaoResult(conteineresCopy, equipamentos);
    }

    private String gerarAlertaPrincipal(CenarioSimulacaoDto cenario, MetricasPatio metricsSimulado) {
        return switch (cenario.getTipoCenario()) {
            case ATRASO_NAVIO -> String.format(
                "Navio atrasará %d horas. Pátio perderá capacidade de %d contêineres.",
                cenario.getHorasAtraso(), metricsSimulado.totalConteineres());
            case MANUTENCAO_EQUIPAMENTO -> String.format(
                "Equipamento %s em manutenção. Apenas %d equipamentos disponíveis.",
                cenario.getCodigoEquipamento(), metricsSimulado.equipamentosDisponiveis());
            case AUMENTO_VOLUME -> String.format(
                "Volume aumentará em %d contêineres. Ocupação do pátio: %d%%",
                cenario.getQuantidadeConteinoresAdicionais(), metricsSimulado.ocupacao());
            default -> "Cenário simulado.";
        };
    }

    private String gerarImpactoProducao(CenarioSimulacaoDto cenario,
                                         MetricasPatio atual,
                                         MetricasPatio simulado) {
        int deltaOcupacao = simulado.ocupacao() - atual.ocupacao();
        double deltaRehandle = simulado.rehandleRatio() - atual.rehandleRatio();

        if (deltaOcupacao > 30) {
            return String.format(
                "CRÍTICO: Ocupação aumentará %d%%. Re-handle aumentará %.1f%%. Sistema operará no limite.",
                deltaOcupacao, deltaRehandle);
        } else if (deltaOcupacao > 15) {
            return String.format(
                "ALTO: Ocupação aumentará %d%%. Re-handle aumentará %.1f%%. Atenção recomendada.",
                deltaOcupacao, deltaRehandle);
        } else {
            return String.format(
                "MODERADO: Ocupação aumentará %d%%. Re-handle aumentará %.1f%%. Sistema absorverá bem.",
                deltaOcupacao, deltaRehandle);
        }
    }

    private String gerarRecomendacao(CenarioSimulacaoDto cenario, MetricasPatio metricsSimulado) {
        return switch (cenario.getTipoCenario()) {
            case ATRASO_NAVIO -> "Confirme atraso com agente marítimo. Prepare baias adicionais. Agendar chegada para próxima janela.";
            case MANUTENCAO_EQUIPAMENTO -> "Escalone manutenção para período de baixa movimentação. Notifique operadores com antecedência.";
            case AUMENTO_VOLUME -> "Ativar plano de contingência. Distribuir volume entre baias. Considerar estacionamento temporário.";
            default -> "Analise o cenário com a operação antes de confirmar.";
        };
    }
}
