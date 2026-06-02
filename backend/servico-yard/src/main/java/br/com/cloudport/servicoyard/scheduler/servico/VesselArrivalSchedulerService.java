package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VesselArrivalSchedulerService {

    private final List<VesselScheduleEntry> agendaNavios = new ArrayList<>();

    public LocalDateTime agendar(VesselArrivalDto navio) {
        LocalDateTime slotDisponivel = encontrarSlotDisponivel(
                navio.getEtaChegada(),
                navio.getJanelaTempoHoras()
        );

        VesselScheduleEntry entry = new VesselScheduleEntry(
                navio.getCodigoNavio(),
                navio.getNomeBerco(),
                slotDisponivel,
                navio.getEtaPartida(),
                navio.getPrioridade()
        );

        agendaNavios.add(entry);
        agendaNavios.sort(Comparator.comparing(VesselScheduleEntry::getTempoPrevisto));

        return slotDisponivel;
    }

    private LocalDateTime encontrarSlotDisponivel(LocalDateTime etaChegada, Integer duracaoHoras) {
        LocalDateTime slotProposto = etaChegada;

        while (existeConflito(slotProposto, slotProposto.plusHours(duracaoHoras))) {
            slotProposto = slotProposto.plusMinutes(30);
        }

        return slotProposto;
    }

    private boolean existeConflito(LocalDateTime inicio, LocalDateTime fim) {
        return agendaNavios.stream()
                .anyMatch(e -> e.temConflito(inicio, fim));
    }

    public List<VesselScheduleEntry> obterAgendaCompleta() {
        return new ArrayList<>(agendaNavios);
    }

    public List<VesselScheduleEntry> obterAgendaProximas24Horas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime amanhaEstaHora = agora.plusHours(24);

        return agendaNavios.stream()
                .filter(e -> !e.getTempoPrevisto().isBefore(agora) &&
                           e.getTempoPrevisto().isBefore(amanhaEstaHora))
                .toList();
    }

    public Integer calcularCapacidadeRequerida(VesselArrivalDto navio) {
        return navio.getQuantidadeContainersImportacao() +
               navio.getQuantidadeContainersExportacao();
    }

    public static class VesselScheduleEntry {
        private String codigoNavio;
        private String nomeBerco;
        private LocalDateTime tempoPrevisto;
        private LocalDateTime tempoTermino;
        private String prioridade;

        public VesselScheduleEntry(String codigoNavio, String nomeBerco,
                                  LocalDateTime tempoPrevisto, LocalDateTime tempoTermino,
                                  String prioridade) {
            this.codigoNavio = codigoNavio;
            this.nomeBerco = nomeBerco;
            this.tempoPrevisto = tempoPrevisto;
            this.tempoTermino = tempoTermino;
            this.prioridade = prioridade != null ? prioridade : "NORMAL";
        }

        public boolean temConflito(LocalDateTime inicio, LocalDateTime fim) {
            return !(fim.isBefore(tempoPrevisto) || inicio.isAfter(tempoTermino));
        }

        public String getCodigoNavio() {
            return codigoNavio;
        }

        public String getNomeBerco() {
            return nomeBerco;
        }

        public LocalDateTime getTempoPrevisto() {
            return tempoPrevisto;
        }

        public LocalDateTime getTempoTermino() {
            return tempoTermino;
        }

        public String getPrioridade() {
            return prioridade;
        }

        public Integer getDuracaoHoras() {
            return (int) java.time.temporal.ChronoUnit.HOURS.between(tempoPrevisto, tempoTermino);
        }

        @Override
        public String toString() {
            return String.format(
                    "Navio: %s | Berço: %s | Início: %s | Duração: %dh | Prioridade: %s",
                    codigoNavio, nomeBerco, tempoPrevisto, getDuracaoHoras(), prioridade
            );
        }
    }
}
