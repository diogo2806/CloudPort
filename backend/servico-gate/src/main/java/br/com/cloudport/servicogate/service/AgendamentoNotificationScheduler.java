package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoNotificationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendamentoNotificationScheduler.class);
    private static final long AHEAD_MINUTES = 30L;

    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoNotificationService notificationService;

    public AgendamentoNotificationScheduler(AgendamentoRepository agendamentoRepository,
                                            AgendamentoNotificationService notificationService) {
        this.agendamentoRepository = agendamentoRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void enviarAlertasDeJanela() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.plusMinutes(AHEAD_MINUTES);
        Set<StatusAgendamento> elegiveis = notificationService.statusesElegiveisParaLembrete();
        List<Agendamento> agendamentos = agendamentoRepository
                .findByHorarioPrevistoChegadaBetweenAndStatusIn(agora, limite, elegiveis);
        agendamentos.forEach(agendamento -> {
            LOGGER.debug("Disparando lembrete de janela para agendamento {}", agendamento.getId());
            notificationService.publicarLembreteJanela(agendamento);
        });
    }
}
