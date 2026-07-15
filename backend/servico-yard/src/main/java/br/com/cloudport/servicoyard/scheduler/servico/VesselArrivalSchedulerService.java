package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.modelo.VesselSchedule;
import br.com.cloudport.servicoyard.scheduler.repositorio.VesselScheduleRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VesselArrivalSchedulerService {

    private final VesselScheduleRepositorio agendaRepositorio;

    public VesselArrivalSchedulerService(VesselScheduleRepositorio agendaRepositorio) {
        this.agendaRepositorio = agendaRepositorio;
    }

    @Transactional
    public synchronized LocalDateTime agendar(VesselArrivalDto navio) {
        validarNavio(navio);
        int duracaoHoras = navio.getJanelaTempoHoras();
        LocalDateTime slotDisponivel = encontrarSlotDisponivel(
                navio.getNomeBerco(),
                navio.getEtaChegada(),
                duracaoHoras
        );

        VesselSchedule entry = new VesselSchedule();
        entry.setCodigoNavio(navio.getCodigoNavio().trim());
        entry.setNomeBerco(navio.getNomeBerco().trim());
        entry.setTempoPrevisto(slotDisponivel);
        entry.setTempoTermino(slotDisponivel.plusHours(duracaoHoras));
        entry.setPrioridade(StringUtils.hasText(navio.getPrioridade()) ? navio.getPrioridade().trim() : "NORMAL");
        entry.setCapacidadeRequerida(calcularCapacidadeRequerida(navio));
        entry.setCriadoEm(LocalDateTime.now());
        agendaRepositorio.save(entry);
        return slotDisponivel;
    }

    private LocalDateTime encontrarSlotDisponivel(String berco,
                                                   LocalDateTime etaChegada,
                                                   Integer duracaoHoras) {
        LocalDateTime slotProposto = etaChegada;
        List<VesselSchedule> agenda = agendaRepositorio.findAllByOrderByTempoPrevistoAsc();
        while (existeConflito(agenda, berco, slotProposto, slotProposto.plusHours(duracaoHoras))) {
            slotProposto = slotProposto.plusMinutes(30);
        }
        return slotProposto;
    }

    private boolean existeConflito(List<VesselSchedule> agenda,
                                   String berco,
                                   LocalDateTime inicio,
                                   LocalDateTime fim) {
        String bercoNormalizado = normalizarBerco(berco);
        return agenda.stream()
                .filter(entry -> normalizarBerco(entry.getNomeBerco()).equals(bercoNormalizado))
                .anyMatch(entry -> inicio.isBefore(entry.getTempoTermino()) && fim.isAfter(entry.getTempoPrevisto()));
    }

    @Transactional(readOnly = true)
    public List<VesselScheduleEntry> obterAgendaCompleta() {
        return agendaRepositorio.findAllByOrderByTempoPrevistoAsc().stream()
                .map(VesselScheduleEntry::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VesselScheduleEntry> obterAgendaProximas24Horas() {
        LocalDateTime agora = LocalDateTime.now();
        return agendaRepositorio
                .findByTempoTerminoAfterAndTempoPrevistoBeforeOrderByTempoPrevistoAsc(agora, agora.plusHours(24))
                .stream()
                .map(VesselScheduleEntry::de)
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
        private final String codigoNavio;
        private final String nomeBerco;
        private final LocalDateTime tempoPrevisto;
        private final LocalDateTime tempoTermino;
        private final String prioridade;
        private final Integer capacidadeRequerida;

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

        static VesselScheduleEntry de(VesselSchedule agenda) {
            return new VesselScheduleEntry(
                    agenda.getCodigoNavio(),
                    agenda.getNomeBerco(),
                    agenda.getTempoPrevisto(),
                    agenda.getTempoTermino(),
                    agenda.getPrioridade(),
                    agenda.getCapacidadeRequerida()
            );
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
