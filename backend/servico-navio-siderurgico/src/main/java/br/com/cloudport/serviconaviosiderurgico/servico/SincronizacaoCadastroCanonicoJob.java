package br.com.cloudport.serviconaviosiderurgico.servico;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "cloudport.runtime.jobs-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SincronizacaoCadastroCanonicoJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoCadastroCanonicoJob.class);
    private static final String CHAVE_EXECUCAO = "cloudport:navio-siderurgico:sincronizacao-cadastro";

    private final NavioSiderurgicoServico navioSiderurgicoServico;
    private final ExecucaoUnicaServico execucaoUnicaServico;
    private final long toleranciaHoras;

    public SincronizacaoCadastroCanonicoJob(
            NavioSiderurgicoServico navioSiderurgicoServico,
            ExecucaoUnicaServico execucaoUnicaServico,
            @Value("${cloudport.integracao.navio.reconciliacao-tolerancia-horas:24}") long toleranciaHoras) {
        this.navioSiderurgicoServico = navioSiderurgicoServico;
        this.execucaoUnicaServico = execucaoUnicaServico;
        this.toleranciaHoras = Math.max(toleranciaHoras, 1L);
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.navio.sincronizacao-ms:3600000}")
    @Transactional
    public void sincronizarCadastroCanonico() {
        boolean executado = execucaoUnicaServico.executarSeDisponivel(
                CHAVE_EXECUCAO,
                this::reconciliarProjecoesDesatualizadas);
        if (!executado) {
            LOGGER.debug("Reconciliacao do cadastro canonico ignorada porque outra instancia possui o bloqueio.");
        }
    }

    private void reconciliarProjecoesDesatualizadas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(toleranciaHoras);
        int atualizados = navioSiderurgicoServico.reconciliarCadastrosDesatualizados(limite);
        if (atualizados > 0) {
            LOGGER.info("Reconciliacao canonica atualizou {} projecao(oes) siderurgica(s).", atualizados);
        }
    }
}
