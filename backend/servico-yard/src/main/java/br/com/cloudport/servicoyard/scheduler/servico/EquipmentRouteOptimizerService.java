package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.EquipmentRouteDto;
import br.com.cloudport.servicoyard.scheduler.dto.EquipmentRouteDto.RouteStopDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EquipmentRouteOptimizerService {

    private static final Integer TEMPO_POR_BLOCO_MINUTOS = 1;
    private static final Integer TEMPO_OPERACAO_MINUTOS = 3;

    public List<EquipmentRouteDto> otimizarRotasEquipamento(
            List<String> equipamentos,
            List<TarefaEquipamento> tarefas) {

        List<EquipmentRouteDto> rotas = new ArrayList<>();

        for (String equipamento : equipamentos) {
            List<TarefaEquipamento> tarefasEquipamento = tarefas.stream()
                    .filter(t -> !t.isAlocada())
                    .limit(10)
                    .toList();

            if (!tarefasEquipamento.isEmpty()) {
                EquipmentRouteDto rota = construirRotaOtimizada(equipamento, tarefasEquipamento);
                rotas.add(rota);
                tarefasEquipamento.forEach(t -> t.setAlocada(true));
            }
        }

        return rotas;
    }

    private EquipmentRouteDto construirRotaOtimizada(String equipamento,
                                                      List<TarefaEquipamento> tarefas) {

        LocalDateTime tempoInicio = LocalDateTime.now();
        EquipmentRouteDto rota = new EquipmentRouteDto(equipamento, tempoInicio, null);

        List<TarefaEquipamento> sequenciaOtimizada = otimizarSequencia(tarefas);
        int distanciaTotal = 0;
        int tempoTotal = 0;
        LocalDateTime tempoAtual = tempoInicio;

        for (int i = 0; i < sequenciaOtimizada.size(); i++) {
            TarefaEquipamento tarefa = sequenciaOtimizada.get(i);

            int distancia = calcularDistancia(i == 0 ? 0 : sequenciaOtimizada.get(i - 1).getLinha(),
                                             i == 0 ? 0 : sequenciaOtimizada.get(i - 1).getColuna(),
                                             tarefa.getLinha(),
                                             tarefa.getColuna());

            int tempoTraslado = distancia * TEMPO_POR_BLOCO_MINUTOS;
            int tempoOperacao = TEMPO_OPERACAO_MINUTOS;
            int tempoTotal_ = tempoTraslado + tempoOperacao;

            RouteStopDto parada = new RouteStopDto(
                    i + 1,
                    tarefa.getTipoOperacao(),
                    tarefa.getCodigoContainer(),
                    tarefa.getLinha(),
                    tarefa.getColuna()
            );

            tempoAtual = tempoAtual.plusMinutes(tempoTotal_);
            parada.setTempoEsperado(tempoAtual);

            rota.adicionarParada(parada);
            distanciaTotal += distancia;
            tempoTotal += tempoTotal_;
        }

        rota.setDistanciaTotal(distanciaTotal);
        rota.setTempoTotalMinutos(tempoTotal);
        rota.setTempoFim(tempoAtual);
        rota.setStatus("PLANEJADO");

        return rota;
    }

    private List<TarefaEquipamento> otimizarSequencia(List<TarefaEquipamento> tarefas) {
        return tarefas.stream()
                .sorted(Comparator.comparingInt(t -> t.getLinha() + t.getColuna()))
                .toList();
    }

    private Integer calcularDistancia(Integer l1, Integer c1, Integer l2, Integer c2) {
        return Math.abs(l1 - l2) + Math.abs(c1 - c2);
    }

    public static class TarefaEquipamento {
        private String codigoContainer;
        private Integer linha;
        private Integer coluna;
        private String tipoOperacao;
        private Boolean alocada;

        public TarefaEquipamento(String codigoContainer, Integer linha, Integer coluna,
                                String tipoOperacao) {
            this.codigoContainer = codigoContainer;
            this.linha = linha;
            this.coluna = coluna;
            this.tipoOperacao = tipoOperacao;
            this.alocada = false;
        }

        public String getCodigoContainer() {
            return codigoContainer;
        }

        public Integer getLinha() {
            return linha;
        }

        public Integer getColuna() {
            return coluna;
        }

        public String getTipoOperacao() {
            return tipoOperacao;
        }

        public Boolean isAlocada() {
            return alocada;
        }

        public void setAlocada(Boolean alocada) {
            this.alocada = alocada;
        }
    }
}
