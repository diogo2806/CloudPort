package br.com.cloudport.servicoyard.scheduler.controller;

import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.PredictiveSchedulerService;
import br.com.cloudport.servicoyard.scheduler.servico.VesselArrivalSchedulerService;
import br.com.cloudport.servicoyard.scheduler.servico.VesselArrivalSchedulerService.VesselScheduleEntry;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','SERVICE_NAVIO')")
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
        return ResponseEntity.ok(vesselScheduler.obterAgendaCompleta());
    }

    @GetMapping("/agenda-proximas-24h")
    public ResponseEntity<List<VesselScheduleEntry>> obterAgendaProximas24Horas() {
        return ResponseEntity.ok(vesselScheduler.obterAgendaProximas24Horas());
    }

    @PostMapping("/gerar-plano")
    public ResponseEntity<SchedulerResultDto> gerarPlanoOperacional(
            @Valid @RequestBody SchedulerPlanoOperacionalRequisicaoDto requisicao) {
        SchedulerResultDto plano = predictiveScheduler.gerarPlanoOperacional(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(plano);
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<String> obterDiagnostico() {
        List<VesselScheduleEntry> agenda = vesselScheduler.obterAgendaProximas24Horas();
        int capacidadeRequerida = agenda.stream()
                .map(VesselScheduleEntry::getCapacidadeRequerida)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        String diagnostico = String.format(
                "Navios nas próximas 24h: %d%nBerços em uso: %d%nMovimentos planejados: %d",
                agenda.size(),
                agenda.stream().map(VesselScheduleEntry::getNomeBerco).distinct().count(),
                capacidadeRequerida
        );
        return ResponseEntity.ok(diagnostico);
    }
}
