package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VesselArrivalSchedulerService {

    private final List<VesselScheduleEntry> agendaNavios = new ArrayList<>();

    public synchronized LocalDateTime agendar(VesselArrivalDto navio) {
        validarNavio(navio);
        int duracaoHoras = navio.getJanelaTempoHoras();
        LocalDateTime slotDisponivel = encontrarSlotDisponivel(
                navio.getNomeBerco(),
                navio.getEtaChegada(),
                duracaoHoras
        );

        VesselScheduleEntry entry = new VesselScheduleEntry(
                navio.getCodigoNavio(),
                navio.getNomeBerco(),
                slotDisponivel,
                slotDisponivel.plusHours(duracaoHoras),
                navio.getPrioridade(),
                calcularCapacidadeRequerida(navio)
        );

        agendaNavios.add(entry);
        agendaNavios.sort(Comparator.comparing(VesselScheduleEntry::getTempoPrevisto));
        return slotDisponivel;
    }

    private LocalDateTime encontrarSlotDisponivel(String berco,
                                                   LocalDateTime etaChegada,
                                                   Integer duracaoHoras) {
        LocalDateTime slotProposto = etaChegada;
        while (existeConflito(berco, slotProposto, slotProposto.plusHours(duracaoHoras))) {
            slotProposto = slotProposto.plusMinutes(30);
        }
        return slotProposto;
    }

    private boolean existeConflito(String berco, LocalDateTime inicio, LocalDateTime fim) {
        String bercoNormalizado = normalizarBerco(berco);
        return agendaNavios.stream()
                .filter(entry -> normalizarBerco(entry.getNomeBerco()).equals(bercoNormalizado))
                .anyMatch(entry -> entry.temConflito(inicio, fim));
    }

    public synchronized List<VesselScheduleEntry> obterAgendaCompleta() {
        return new ArrayList<>(agendaNavios);
    }

    public synchronized List<VesselScheduleEntry> obterAgendaProximas24Horas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime amanhaEstaHora = agora.plusHours(24);
        return agendaNavios.stream()
                .filter(entry -> entry.getTempoTermino().isAfter(agora)
                        && entry.getTempoPrevisto().isBefore(amanhaEstaHora))
                .toList();
    }

    public Integer calcularCapacidadeRequerida(VesselArrivalDto navio) {
        int importacao = navio.getQuantidadeContainersImportacao() == null ? 0 : navio.getQuantidadeContainersImportacao();
        int exportacao = navio.getQuantidadeContainersExportacao() == null ? 0 : navio.getQuantidadeContainersExportacao();
        return importacao + exportacao;
    }

    private void validarNavio(VesselArrivalDto navio) {
        if (navio == null || !StringUtils.hasText(navio.getCodigoNavio()) || !StringUtils.hasText(navio.getNomeBerco())) {
            throw new IllegalArgumentException("Navio e berço devem ser informados.");
        }
        if (navio.getEtaChegada() == null || navio.getEtaPartida() == null
                || !navio.getEtaPartida().isAfter(navio.getEtaChegada())) {
            throw new IllegalArgumentException("A ETA de partida deve ser posterior à ETA de chegada.");
        }
    }

    private String normalizarBerco(String berco) {
        return StringUtils.hasText(berco) ? berco.trim().toUpperCase(Locale.ROOT) : "SEM_BERCO";
    }

    public static class VesselScheduleEntry {
        private String codigoNavio;
        private String nomeBerco;
        private LocalDateTime tempoPrevisto;
        private LocalDateTime tempoTermino;
        private String prioridade;
        private Integer capacidadeRequerida;

        public VesselScheduleEntry(String codigoNavio,
                                   String nomeBerco,
                                   LocalDateTime tempoPrevisto,
                                   LocalDateTime tempoTermino,
                                   String prioridade,
                                   Integer capacidadeRequerida) {
            this.codigoNavio = codigoNavio;
            this.nomeBerco = nomeBerco;
            this.tempoPrevisto = tempoPrevisto;
            this.tempoTermino = tempoTermino;
            this.prioridade = prioridade != null ? prioridade : "NORMAL";
            this.capacidadeRequerida = capacidadeRequerida == null ? 0 : capacidadeRequerida;
        }

        public boolean temConflito(LocalDateTime inicio, LocalDateTime fim) {
            return inicio.isBefore(tempoTermino) && fim.isAfter(tempoPrevisto);
        }

        public String getCodigoNavio() { return codigoNavio; }
        public String getNomeBerco() { return nomeBerco; }
        public LocalDateTime getTempoPrevisto() { return tempoPrevisto; }
        public LocalDateTime getTempoTermino() { return tempoTermino; }
        public String getPrioridade() { return prioridade; }
        public Integer getCapacidadeRequerida() { return capacidadeRequerida; }

        public Integer getDuracaoHoras() {
            return (int) java.time.temporal.ChronoUnit.HOURS.between(tempoPrevisto, tempoTermino);
        }

        @Override
        public String toString() {
            return String.format(
                    "Navio: %s | Berço: %s | Início: %s | Duração: %dh | Capacidade: %d | Prioridade: %s",
                    codigoNavio, nomeBerco, tempoPrevisto, getDuracaoHoras(), capacidadeRequerida, prioridade
            );
        }
    }
}
