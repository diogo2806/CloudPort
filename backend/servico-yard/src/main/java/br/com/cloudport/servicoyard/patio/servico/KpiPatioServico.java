package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.KpiPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;

    public KpiPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                           EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                           MovimentoPatioRepositorio movimentoPatioRepositorio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
    }

    @Transactional(readOnly = true)
    public KpiPatioDto calcularKpis() {
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAll();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();
        List<MovimentoPatio> movimentos = movimentoPatioRepositorio.findAll();

        double yardDensity = calcularYardDensity(conteineres);
        double rehandleRatio = calcularRehandleRatio(conteineres);
        double equipmentUtilization = calcularEquipmentUtilization(equipamentos, movimentos);
        int gateThroghput = calcularGateThroghput(movimentos);

        return new KpiPatioDto(yardDensity, rehandleRatio, equipmentUtilization,
            gateThroghput, LocalDateTime.now());
    }

    private double calcularYardDensity(List<ConteinerPatio> conteineres) {
        if (conteineres.isEmpty()) {
            return 0.0;
        }

        int totalPosicoes = conteineres.stream()
            .map(c -> c.getPosicao().getLinha() * 100 + c.getPosicao().getColuna())
            .distinct()
            .toArray()
            .length;

        int capacidadeTotalEstimada = 500;
        int conteineresPorPosicao = 4;

        return (conteineres.size() * 100.0) / (capacidadeTotalEstimada * conteineresPorPosicao);
    }

    private double calcularRehandleRatio(List<ConteinerPatio> conteineres) {
        if (conteineres.isEmpty()) {
            return 0.0;
        }

        long totalRehandles = 0;
        for (ConteinerPatio conteiner : conteineres) {
            long conteineresCimaDoAtual = conteineres.stream()
                .filter(c -> c.getPosicao().getColuna().equals(conteiner.getPosicao().getColuna()))
                .filter(c -> c.getPosicao().getLinha() < conteiner.getPosicao().getLinha())
                .count();
            totalRehandles += conteineresCimaDoAtual;
        }

        return (totalRehandles * 100.0) / conteineres.size();
    }

    private double calcularEquipmentUtilization(List<EquipamentoPatio> equipamentos,
                                                  List<MovimentoPatio> movimentos) {
        if (equipamentos.isEmpty()) {
            return 0.0;
        }

        long equipamentosOperacionais = equipamentos.stream()
            .filter(e -> e.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
            .count();

        if (equipamentosOperacionais == 0) {
            return 0.0;
        }

        long tarefasUltimaHora = movimentos.stream()
            .filter(m -> m.getRegistradoEm().isAfter(LocalDateTime.now().minusHours(1)))
            .count();

        double capacidadeHorariaPorEquipamento = 15.0;
        double capacidadeTotalHoraria = equipamentosOperacionais * capacidadeHorariaPorEquipamento;

        return (tarefasUltimaHora * 100.0) / Math.max(1, (int) capacidadeTotalHoraria);
    }

    private int calcularGateThroghput(List<MovimentoPatio> movimentos) {
        long tarefasUltimaHora = movimentos.stream()
            .filter(m -> m.getRegistradoEm().isAfter(LocalDateTime.now().minusHours(1)))
            .count();

        return (int) tarefasUltimaHora;
    }
}
