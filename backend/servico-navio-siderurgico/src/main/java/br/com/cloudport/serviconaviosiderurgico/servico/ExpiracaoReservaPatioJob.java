package br.com.cloudport.serviconaviosiderurgico.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "cloudport.runtime.jobs-enabled",
        havingValue = "true")
public class ExpiracaoReservaPatioJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiracaoReservaPatioJob.class);
    private static final String CHAVE_EXECUCAO = "cloudport:navio-patio:expiracao-reservas";

    private final ReservaPatioNavioServico reservaServico;
    private final ExecucaoUnicaServico execucaoUnicaServico;

    public ExpiracaoReservaPatioJob(ReservaPatioNavioServico reservaServico,
                                    ExecucaoUnicaServico execucaoUnicaServico) {
        this.reservaServico = reservaServico;
        this.execucaoUnicaServico = execucaoUnicaServico;
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.yard.reserva-expiracao-ms:60000}")
    @Transactional
    public void expirarReservasVencidas() {
        boolean executado = execucaoUnicaServico.executarSeDisponivel(
                CHAVE_EXECUCAO,
                this::expirarSemBloqueio);
        if (!executado) {
            LOGGER.debug("Expiracao de reservas ignorada porque outra instancia possui o bloqueio.");
        }
    }

    private void expirarSemBloqueio() {
        int expiradas = reservaServico.expirarReservasVencidas();
        if (expiradas > 0) {
            LOGGER.info("Expiracao automatica alterou {} reserva(s) de patio.", expiradas);
        }
    }
}
