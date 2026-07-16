package br.com.cloudport.servicoyard.edi.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EdiProcessamentoWorkerCondicaoTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(EdiProcessamentoWorkerServico.class,
                    () -> mock(EdiProcessamentoWorkerServico.class))
            .withBean(EdiAuditoriaServico.class,
                    () -> mock(EdiAuditoriaServico.class))
            .withUserConfiguration(EdiProcessamentoWorker.class);

    @Test
    void naoDeveCriarWorkerQuandoJobsEstaoDesabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> assertThat(contexto)
                        .doesNotHaveBean(EdiProcessamentoWorker.class));
    }

    @Test
    void deveCriarWorkerQuandoJobsEstaoHabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> assertThat(contexto)
                        .hasSingleBean(EdiProcessamentoWorker.class));
    }

    @Test
    void deveCriarWorkerPorPadraoParaPreservarExecucaoStandalone() {
        contextRunner.run(contexto -> assertThat(contexto)
                .hasSingleBean(EdiProcessamentoWorker.class));
    }
}
