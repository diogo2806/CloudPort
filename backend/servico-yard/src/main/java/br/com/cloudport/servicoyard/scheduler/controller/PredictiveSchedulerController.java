package br.com.cloudport.servicoyard.scheduler.controller;

import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.PredictiveSchedulerService;
import br.com.cloudport.servicoyard.scheduler.servico.VesselArrivalSchedulerService;
import br.com.cloudport.servicoyard.scheduler.servico.VesselArrivalSchedulerService.VesselScheduleEntry;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
public class PredictiveSchedulerController {

    private final PredictiveSchedulerService predictiveScheduler;
    private final VesselArrivalSchedulerService vesselScheduler;

    public PredictiveSchedulerController(
            PredictiveSchedulerService predictiveScheduler,
            VesselArrivalSchedulerService vesselScheduler) {
        this.predictiveScheduler = predictiveScheduler;
        this.vesselScheduler = vesselScheduler;
    }

    @PostMapping("/vessel-arrival")
    public ResponseEntity<String> agendarChegadaNavio(
            @Valid @RequestBody VesselArrivalDto navio) {

        String mensagem = vesselScheduler.agendar(navio).toString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Navio agendado: " + mensagem);
    }

    @GetMapping("/agenda-completa")
    public ResponseEntity<List<VesselScheduleEntry>> obterAgendaCompleta() {
        List<VesselScheduleEntry> agenda = vesselScheduler.obterAgendaCompleta();
        return ResponseEntity.ok(agenda);
    }

    @GetMapping("/agenda-proximas-24h")
    public ResponseEntity<List<VesselScheduleEntry>> obterAgendaProximas24Horas() {
        List<VesselScheduleEntry> agenda = vesselScheduler.obterAgendaProximas24Horas();
        return ResponseEntity.ok(agenda);
    }

    @PostMapping("/gerar-plano")
    public ResponseEntity<SchedulerResultDto> gerarPlanoOperacional(
            @Valid @RequestBody VesselArrivalDto navio,
            @RequestParam(defaultValue = "5") Integer numeroEquipamentos,
            @RequestParam(defaultValue = "10") Integer containersPorEquipamento) {

        SchedulerResultDto plano = predictiveScheduler.gerarPlanoOperacionalDetalhado(
                navio,
                numeroEquipamentos,
                containersPorEquipamento
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(plano);
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<String> obterDiagnostico() {
        List<VesselScheduleEntry> agenda = vesselScheduler.obterAgendaProximas24Horas();
        String diagnostico = String.format(
                "Navios na próximas 24h: %d\n" +
                "Berços em uso: %d\n" +
                "Capacidade estimada: %d containers",
                agenda.size(),
                agenda.stream().map(VesselScheduleEntry::getNomeBerco).distinct().count(),
                agenda.stream()
                    .mapToInt(e -> e.getDuracaoHoras() * 20)
                    .sum()
        );

        return ResponseEntity.ok(diagnostico);
    }
}
