package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.AlertaPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertasPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;

    private long tempoParadoHoras = 4L;

    public AlertasPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                               EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                               MovimentoPatioRepositorio movimentoPatioRepositorio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
    }

    @Value("${cloudport.yard.alertas.tempo-parado-horas:4}")
    public void setTempoParadoHoras(long tempoParadoHoras) {
        this.tempoParadoHoras = Math.max(1L, tempoParadoHoras);
    }

    @Transactional(readOnly = true)
    public List<AlertaPatioDto> calcularAlertas() {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();

        alertas.addAll(calcularRehandles(conteineres));
        alertas.addAll(calcularConflitosInfraestrutura(conteineres, equipamentos));
        alertas.addAll(calcularGargolosEquipamento(equipamentos));
        alertas.addAll(calcularAlertasPermanencia(conteineres, equipamentos));

        return alertas;
    }

    private List<AlertaPatioDto> calcularRehandles(List<ConteinerPatio> conteineres) {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        for (ConteinerPatio conteiner : conteineres) {
            int rehandlesNecessarios = calcularRehandlesParaConteiner(conteiner, conteineres);
            if (rehandlesNecessarios > 2) {
                AlertaPatioDto.NivelSeveridade nivel = rehandlesNecessarios > 4
                        ? AlertaPatioDto.NivelSeveridade.CRITICO
                        : AlertaPatioDto.NivelSeveridade.ATENCAO;
                alertas.add(new AlertaPatioDto(
                        conteiner.getId(), conteiner.getCodigo(), AlertaPatioDto.TipoAlerta.REHANDLE, nivel,
                        String.format("Contêiner %s exigirá %d movimentações extras para acesso.",
                                conteiner.getCodigo(), rehandlesNecessarios),
                        String.format("Considere reposicionar contêineres acima de %s ou planejá-lo para próxima janela.",
                                conteiner.getCodigo())));
            }
        }
        return alertas;
    }

    private int calcularRehandlesParaConteiner(ConteinerPatio conteiner, List<ConteinerPatio> todosConteineres) {
        if (conteiner.getPosicao() == null) {
            return 0;
        }
        int mesmaLinha = (int) todosConteineres.stream()
                .filter(c -> c.getPosicao() != null)
                .filter(c -> c.getPosicao().getLinha().equals(conteiner.getPosicao().getLinha()))
                .filter(c -> c.getPosicao().getColuna().equals(conteiner.getPosicao().getColuna()))
                .count();
        int mesmaColuna = (int) todosConteineres.stream()
                .filter(c -> c.getPosicao() != null)
                .filter(c -> c.getPosicao().getColuna().equals(conteiner.getPosicao().getColuna()))
                .filter(c -> c.getPosicao().getLinha() < conteiner.getPosicao().getLinha())
                .count();
        return mesmaLinha - 1 + mesmaColuna;
    }

    private List<AlertaPatioDto> calcularConflitosInfraestrutura(List<ConteinerPatio> conteineres,
                                                                   List<EquipamentoPatio> equipamentos) {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        Map<String, EquipamentoPatio> equipamentosComEnergia = equipamentos.stream()
                .filter(e -> e.getTipoEquipamento() == TipoEquipamento.RTG)
                .collect(Collectors.toMap(e -> e.getLinha() + "_" + e.getColuna(), e -> e, (a, b) -> a));

        for (ConteinerPatio conteiner : conteineres) {
            if (conteiner.getPosicao() != null && isReeferOuRefrigerado(conteiner)) {
                String posicaoKey = conteiner.getPosicao().getLinha() + "_" + conteiner.getPosicao().getColuna();
                if (!equipamentosComEnergia.containsKey(posicaoKey)) {
                    alertas.add(new AlertaPatioDto(
                            conteiner.getId(), conteiner.getCodigo(),
                            AlertaPatioDto.TipoAlerta.CONFLITO_INFRAESTRUTURA,
                            AlertaPatioDto.NivelSeveridade.CRITICO,
                            String.format("Contêiner refrigerado %s alocado em posição (%d, %d) sem conexão de energia.",
                                    conteiner.getCodigo(), conteiner.getPosicao().getLinha(), conteiner.getPosicao().getColuna()),
                            "Realoque o contêiner para uma baía com conexão de energia ou conecte um gerador."));
                }
            }
        }
        return alertas;
    }

    private List<AlertaPatioDto> calcularGargolosEquipamento(List<EquipamentoPatio> equipamentos) {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime proximaHora = agora.plusHours(1);
        long tarefasProximaHora = movimentoPatioRepositorio.findAll().stream()
                .filter(m -> m.getRegistradoEm().isAfter(agora) && m.getRegistradoEm().isBefore(proximaHora))
                .count();

        for (EquipamentoPatio equipamento : equipamentos) {
            if (equipamento.getTipoEquipamento() == TipoEquipamento.RTG) {
                int capacidadeHoraria = 15;
                if (tarefasProximaHora > capacidadeHoraria) {
                    double percentualCarga = (tarefasProximaHora * 100.0) / capacidadeHoraria;
                    AlertaPatioDto.NivelSeveridade nivel = percentualCarga > 150
                            ? AlertaPatioDto.NivelSeveridade.CRITICO
                            : AlertaPatioDto.NivelSeveridade.ATENCAO;
                    alertas.add(new AlertaPatioDto(
                            null, equipamento.getIdentificador(), AlertaPatioDto.TipoAlerta.GARGALO_EQUIPAMENTO,
                            nivel,
                            String.format("RTG %s sobrecarregado. %d tarefas agendadas para a próxima hora (capacidade: %d).",
                                    equipamento.getIdentificador(), tarefasProximaHora, capacidadeHoraria),
                            "Distribua cargas para outros RTGs ou adie operações de menor prioridade."));
                }
            }
        }
        return alertas;
    }

    private List<AlertaPatioDto> calcularAlertasPermanencia(List<ConteinerPatio> conteineres,
                                                              List<EquipamentoPatio> equipamentos) {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        LocalDateTime limite = LocalDateTime.now().minusHours(tempoParadoHoras);

        List<ConteinerPatio> veiculosParados = conteineres.stream()
                .filter(item -> item.getAtualizadoEm() != null && item.getAtualizadoEm().isBefore(limite))
                .collect(Collectors.toList());
        if (!veiculosParados.isEmpty()) {
            ConteinerPatio maisAntigo = veiculosParados.stream()
                    .min(Comparator.comparing(ConteinerPatio::getAtualizadoEm)).orElse(veiculosParados.get(0));
            long horas = Duration.between(maisAntigo.getAtualizadoEm(), LocalDateTime.now()).toHours();
            String regioes = veiculosParados.stream().map(ConteinerPatio::getDestino).filter(Objects::nonNull)
                    .distinct().limit(5).collect(Collectors.joining(", "));
            alertas.add(new AlertaPatioDto(
                    null, "VEICULOS_PARADOS", AlertaPatioDto.TipoAlerta.VEICULOS_PARADOS,
                    severidadePermanencia(horas),
                    String.format("Veículos parados: %d. Regiões: %s. Maior permanência: %dh.",
                            veiculosParados.size(), regioes.isEmpty() ? "não informada" : regioes, horas),
                    "/yard/inventory?status=parado&tipo=veiculo"));
        }

        List<EquipamentoPatio> equipamentosParados = equipamentos.stream()
                .filter(item -> item.getAtualizadoEm() != null && item.getAtualizadoEm().isBefore(limite))
                .collect(Collectors.toList());
        if (!equipamentosParados.isEmpty()) {
            EquipamentoPatio maisAntigo = equipamentosParados.stream()
                    .min(Comparator.comparing(EquipamentoPatio::getAtualizadoEm)).orElse(equipamentosParados.get(0));
            long horas = Duration.between(maisAntigo.getAtualizadoEm(), LocalDateTime.now()).toHours();
            String regioes = equipamentosParados.stream()
                    .map(item -> item.getLinha() + "/" + item.getColuna()).distinct().limit(5)
                    .collect(Collectors.joining(", "));
            alertas.add(new AlertaPatioDto(
                    null, "EQUIPAMENTOS_PARADOS", AlertaPatioDto.TipoAlerta.EQUIPAMENTOS_PARADOS,
                    severidadePermanencia(horas),
                    String.format("Equipamentos parados: %d. Regiões: %s. Maior permanência: %dh.",
                            equipamentosParados.size(), regioes, horas),
                    "/yard/inventory?status=parado&tipo=equipamento"));
        }
        return alertas;
    }

    private AlertaPatioDto.NivelSeveridade severidadePermanencia(long horas) {
        return horas >= tempoParadoHoras * 2
                ? AlertaPatioDto.NivelSeveridade.CRITICO
                : AlertaPatioDto.NivelSeveridade.ATENCAO;
    }

    private boolean isReeferOuRefrigerado(ConteinerPatio conteiner) {
        if (conteiner.getCarga() == null) {
            return false;
        }
        String descricao = conteiner.getCarga().getDescricao() != null
                ? conteiner.getCarga().getDescricao().toUpperCase()
                : "";
        return descricao.contains("REEFER") || descricao.contains("REFRIGERADO") || descricao.contains("PERIGOSA");
    }
}
