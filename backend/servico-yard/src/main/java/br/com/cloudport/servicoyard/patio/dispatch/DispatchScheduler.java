package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Rota;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

public interface DispatchScheduler {

    Avaliacao avaliar(OrdemTrabalhoPatio ordem,
                      EquipamentoPatio equipamento,
                      Configuracao configuracao,
                      Rota rota,
                      LocalDateTime referencia);

    record Avaliacao(double score, boolean elegivel, List<String> motivosBloqueio, String memoriaCalculo) {
    }
}

@Component
class DispatchSchedulerRegistry {

    private final Map<TipoEquipamento, DispatchScheduler> schedulers = new EnumMap<>(TipoEquipamento.class);

    DispatchSchedulerRegistry() {
        DispatchScheduler horizontal = new SchedulerHorizontal();
        DispatchScheduler vertical = new SchedulerVertical();
        DispatchScheduler cais = new SchedulerCais();
        DispatchScheduler ferroviario = new SchedulerFerroviario();
        schedulers.put(TipoEquipamento.TRATOR_PORTUARIO, horizontal);
        schedulers.put(TipoEquipamento.STRADDLE_CARRIER, horizontal);
        schedulers.put(TipoEquipamento.RTG, vertical);
        schedulers.put(TipoEquipamento.RMG, vertical);
        schedulers.put(TipoEquipamento.ASC, vertical);
        schedulers.put(TipoEquipamento.REACH_STACKER, vertical);
        schedulers.put(TipoEquipamento.GUINDASTE_SHIP_TO_SHORE, cais);
        schedulers.put(TipoEquipamento.EQUIPAMENTO_FERROVIARIO, ferroviario);
    }

    DispatchScheduler obter(TipoEquipamento tipoEquipamento) {
        return schedulers.getOrDefault(tipoEquipamento, new SchedulerVertical());
    }

    private abstract static class SchedulerBase implements DispatchScheduler {

        @Override
        public Avaliacao avaliar(OrdemTrabalhoPatio ordem,
                                 EquipamentoPatio equipamento,
                                 Configuracao configuracao,
                                 Rota rota,
                                 LocalDateTime referencia) {
            int prioridade = ordem.getPrioridadeOperacional() == null
                    ? 50 : Math.max(0, ordem.getPrioridadeOperacional());
            double componentePrioridade = Math.max(0, 100 - prioridade)
                    * configuracao.pesoPrioridade();
            if (ordem.isPrioridadeBusca()) {
                componentePrioridade += 25 * configuracao.pesoPrioridade();
            }
            long atrasoMinutos = ordem.getCriadoEm() == null
                    ? 0 : Math.max(0, Duration.between(ordem.getCriadoEm(), referencia).toMinutes());
            double componenteAtraso = Math.min(1440, atrasoMinutos) * configuracao.pesoAtraso();
            double componenteDistancia = rota.distanciaMetros() * configuracao.pesoDistancia();
            double componenteCongestionamento = rota.congestionamentoPercentual()
                    * configuracao.pesoCongestionamento();
            double ajusteFamilia = ajusteFamilia(ordem, equipamento, rota);
            double score = componentePrioridade + componenteAtraso + ajusteFamilia
                    - componenteDistancia - componenteCongestionamento;
            List<String> bloqueios = bloqueios(configuracao, rota);
            String memoria = "familia=" + equipamento.getTipoEquipamento()
                    + "; prioridade=" + arredondar(componentePrioridade)
                    + "; atraso=" + arredondar(componenteAtraso)
                    + "; distancia=-" + arredondar(componenteDistancia)
                    + "; congestionamento=-" + arredondar(componenteCongestionamento)
                    + "; ajusteFamilia=" + arredondar(ajusteFamilia)
                    + "; score=" + arredondar(score)
                    + "; " + rota.justificativa();
            return new Avaliacao(arredondar(score), bloqueios.isEmpty(), bloqueios, memoria);
        }

        protected abstract double ajusteFamilia(OrdemTrabalhoPatio ordem,
                                                 EquipamentoPatio equipamento,
                                                 Rota rota);

        private List<String> bloqueios(Configuracao configuracao, Rota rota) {
            java.util.ArrayList<String> bloqueios = new java.util.ArrayList<>();
            if (rota.bloqueada()) {
                bloqueios.add("Rota bloqueada, interditada ou com limite regional de CHE atingido.");
            }
            if (configuracao.modo() == DispatchEnums.ModoDispatch.AUTOMATICO
                    && rota.telemetriaAtrasada()) {
                bloqueios.add("Telemetria ausente ou fora da tolerancia para dispatch automatico.");
            }
            return List.copyOf(bloqueios);
        }

        protected double arredondar(double valor) {
            return Math.round(valor * 100.0) / 100.0;
        }
    }

    private static final class SchedulerHorizontal extends SchedulerBase {
        @Override
        protected double ajusteFamilia(OrdemTrabalhoPatio ordem,
                                        EquipamentoPatio equipamento,
                                        Rota rota) {
            double bonusTransferencia = ordem.getTipoMovimento() != null
                    && ordem.getTipoMovimento().name().contains("TRANSFER") ? 80.0 : 25.0;
            return bonusTransferencia - rota.etaSegundos() * 0.05;
        }
    }

    private static final class SchedulerVertical extends SchedulerBase {
        @Override
        protected double ajusteFamilia(OrdemTrabalhoPatio ordem,
                                        EquipamentoPatio equipamento,
                                        Rota rota) {
            double camada = ordem.getCamadaDestino() == null ? 0.0
                    : Math.min(50.0, ordem.getCamadaDestino().length() * 4.0);
            return 60.0 - camada;
        }
    }

    private static final class SchedulerCais extends SchedulerBase {
        @Override
        protected double ajusteFamilia(OrdemTrabalhoPatio ordem,
                                        EquipamentoPatio equipamento,
                                        Rota rota) {
            double sequencia = ordem.getSequenciaNavio() == null
                    ? 0.0 : Math.max(0, 120 - ordem.getSequenciaNavio());
            return 100.0 + sequencia;
        }
    }

    private static final class SchedulerFerroviario extends SchedulerBase {
        @Override
        protected double ajusteFamilia(OrdemTrabalhoPatio ordem,
                                        EquipamentoPatio equipamento,
                                        Rota rota) {
            String origem = ordem.getTipoOrigem() == null ? "" : ordem.getTipoOrigem();
            String destino = ordem.getTipoDestino() == null ? "" : ordem.getTipoDestino();
            return origem.toUpperCase().contains("RAIL") || destino.toUpperCase().contains("RAIL")
                    ? 140.0 : -100.0;
        }
    }
}
