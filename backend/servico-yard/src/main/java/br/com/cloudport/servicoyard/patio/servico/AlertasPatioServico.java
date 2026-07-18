package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.AlertaPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertasPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;

    public AlertasPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                               EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                               MovimentoPatioRepositorio movimentoPatioRepositorio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
    }

    @Transactional(readOnly = true)
    public List<AlertaPatioDto> calcularAlertas() {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();

        alertas.addAll(calcularRehandles(conteineres));
        alertas.addAll(calcularConflitosInfraestrutura(conteineres, equipamentos));
        alertas.addAll(calcularGargolosEquipamento(equipamentos, conteineres));

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

                AlertaPatioDto alerta = new AlertaPatioDto(
                    conteiner.getId(),
                    conteiner.getCodigo(),
                    AlertaPatioDto.TipoAlerta.REHANDLE,
                    nivel,
                    String.format("Contêiner %s exigirá %d movimentações extras para acesso.",
                        conteiner.getCodigo(), rehandlesNecessarios),
                    String.format("Considere reposicionar contêineres acima de %s ou planejá-lo para próxima janela.",
                        conteiner.getCodigo())
                );
                alertas.add(alerta);
            }
        }

        return alertas;
    }

    private int calcularRehandlesParaConteiner(ConteinerPatio conteiner, List<ConteinerPatio> todosConteineres) {
        int mesmaLinha = (int) todosConteineres.stream()
            .filter(c -> c.getPosicao().getLinha().equals(conteiner.getPosicao().getLinha()))
            .filter(c -> c.getPosicao().getColuna().equals(conteiner.getPosicao().getColuna()))
            .count();

        int mesmaColuna = (int) todosConteineres.stream()
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
            .collect(Collectors.toMap(
                e -> e.getLinha() + "_" + e.getColuna(),
                e -> e
            ));

        for (ConteinerPatio conteiner : conteineres) {
            if (isReeferOuRefrigerado(conteiner)) {
                String posicaoKey = conteiner.getPosicao().getLinha() + "_" + conteiner.getPosicao().getColuna();
                if (!equipamentosComEnergia.containsKey(posicaoKey)) {
                    AlertaPatioDto alerta = new AlertaPatioDto(
                        conteiner.getId(),
                        conteiner.getCodigo(),
                        AlertaPatioDto.TipoAlerta.CONFLITO_INFRAESTRUTURA,
                        AlertaPatioDto.NivelSeveridade.CRITICO,
                        String.format("Contêiner refrigerado %s alocado em posição (%d, %d) sem conexão de energia.",
                            conteiner.getCodigo(), conteiner.getPosicao().getLinha(), conteiner.getPosicao().getColuna()),
                        "Realoque o contêiner para uma baía com conexão de energia ou conecte um gerador."
                    );
                    alertas.add(alerta);
                }
            }
        }

        return alertas;
    }

    private List<AlertaPatioDto> calcularGargolosEquipamento(List<EquipamentoPatio> equipamentos,
                                                               List<ConteinerPatio> conteineres) {
        List<AlertaPatioDto> alertas = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime proximaHora = agora.plusHours(1);

        for (EquipamentoPatio equipamento : equipamentos) {
            if (equipamento.getTipoEquipamento() == TipoEquipamento.RTG) {
                long tarefasProximaHora = movimentoPatioRepositorio.findAll().stream()
                    .filter(m -> m.getRegistradoEm().isAfter(agora) && m.getRegistradoEm().isBefore(proximaHora))
                    .count();

                int capacidadeHoraria = 15;
                if (tarefasProximaHora > capacidadeHoraria) {
                    double percentualCarga = (tarefasProximaHora * 100.0) / capacidadeHoraria;
                    AlertaPatioDto.NivelSeveridade nivel = percentualCarga > 150
                        ? AlertaPatioDto.NivelSeveridade.CRITICO
                        : AlertaPatioDto.NivelSeveridade.ATENCAO;

                    AlertaPatioDto alerta = new AlertaPatioDto(
                        null,
                        equipamento.getIdentificador(),
                        AlertaPatioDto.TipoAlerta.GARGALO_EQUIPAMENTO,
                        nivel,
                        String.format("RTG %s sobrecarregado. %d tarefas agendadas para a próxima hora (capacidade: %d).",
                            equipamento.getIdentificador(), tarefasProximaHora, capacidadeHoraria),
                        "Distribua cargas para outros RTGs ou adie operações de menor prioridade."
                    );
                    alertas.add(alerta);
                }
            }
        }

        return alertas;
    }

    private boolean isReeferOuRefrigerado(ConteinerPatio conteiner) {
        if (conteiner.getCarga() == null) {
            return false;
        }
        String descricao = (conteiner.getCarga().getDescricao() != null)
            ? conteiner.getCarga().getDescricao().toUpperCase()
            : "";
        return descricao.contains("REEFER") || descricao.contains("REFRIGERADO") || descricao.contains("PERIGOSA");
    }
}
