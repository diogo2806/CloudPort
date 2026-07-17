package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class PredictiveReshuffflingJobTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(PredictiveReshuffflingServico.class,
                    () -> mock(PredictiveReshuffflingServico.class))
            .withUserConfiguration(PredictiveReshuffflingJob.class);

    @Test
    void naoDeveCriarJobQuandoAgendamentosEstaoDesabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> assertThat(contexto)
                        .doesNotHaveBean(PredictiveReshuffflingJob.class));
    }

    @Test
    void naoDeveCriarJobQuandoPropriedadeNaoFoiInformada() {
        contextRunner.run(contexto -> assertThat(contexto)
                .doesNotHaveBean(PredictiveReshuffflingJob.class));
    }

    @Test
    void deveCriarJobQuandoAgendamentosEstaoHabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> assertThat(contexto)
                        .hasSingleBean(PredictiveReshuffflingJob.class));
    }

    @Test
    void deveDelegarExecucaoAoServico() {
        PredictiveReshuffflingServico servico = mock(PredictiveReshuffflingServico.class);
        PredictiveReshuffflingJob job = new PredictiveReshuffflingJob(servico);

        job.executar();

        verify(servico).executarReshuffflingNoturno();
    }

    @Test
    void deveConcentrarAnotacaoDeAgendamentoNoJob() throws NoSuchMethodException {
        Method executarJob = PredictiveReshuffflingJob.class.getDeclaredMethod("executar");
        Method executarServico = PredictiveReshuffflingServico.class
                .getDeclaredMethod("executarReshuffflingNoturno");

        Scheduled scheduled = executarJob.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 2 * * ?");
        assertThat(executarServico.getAnnotation(Scheduled.class)).isNull();
    }
}
