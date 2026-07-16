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
        havingValue = "true",
        matchIfMissing = true)
public class SincronizacaoCadastroCanonicoJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoCadastroCanonicoJob.class);
    private static final String CHAVE_EXECUCAO = "cloudport:navio-siderurgico:sincronizacao-cadastro";

    private final NavioSiderurgicoServico navioSiderurgicoServico;
    private final ExecucaoUnicaServico execucaoUnicaServico;

    public SincronizacaoCadastroCanonicoJob(NavioSiderurgicoServico navioSiderurgicoServico,
                                            ExecucaoUnicaServico execucaoUnicaServico) {
        this.navioSiderurgicoServico = navioSiderurgicoServico;
        this.execucaoUnicaServico = execucaoUnicaServico;
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.navio.sincronizacao-ms:300000}")
    @Transactional
    public void sincronizarCadastroCanonico() {
        boolean executado = execucaoUnicaServico.executarSeDisponivel(
                CHAVE_EXECUCAO,
                navioSiderurgicoServico::sincronizarCadastroCanonico);
        if (!executado) {
            LOGGER.debug("Sincronizacao do cadastro canonico ignorada porque outra instancia possui o bloqueio.");
        }
    }
}
