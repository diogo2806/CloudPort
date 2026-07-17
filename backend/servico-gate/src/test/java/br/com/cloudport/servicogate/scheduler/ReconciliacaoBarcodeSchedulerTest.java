package br.com.cloudport.servicogate.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class ReconciliacaoBarcodeSchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ReconciliacaoBarcodeJob.class, () -> mock(ReconciliacaoBarcodeJob.class))
            .withUserConfiguration(ReconciliacaoBarcodeScheduler.class);

    @Test
    void naoDeveRegistrarCronQuandoJobsEstaoDesabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> assertThat(contexto)
                        .doesNotHaveBean(ReconciliacaoBarcodeScheduler.class));
    }

    @Test
    void naoDeveRegistrarCronQuandoControleCanonicoNaoFoiInformado() {
        contextRunner.run(contexto -> assertThat(contexto)
                .doesNotHaveBean(ReconciliacaoBarcodeScheduler.class));
    }

    @Test
    void deveRegistrarCronQuandoJobsEstaoHabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> assertThat(contexto)
                        .hasSingleBean(ReconciliacaoBarcodeScheduler.class));
    }

    @Test
    void deveDelegarAoCasoDeUsoSemOcultarFalha() {
        ReconciliacaoBarcodeJob job = mock(ReconciliacaoBarcodeJob.class);
        ReconciliacaoBarcodeScheduler scheduler = new ReconciliacaoBarcodeScheduler(job);
        doThrow(new IllegalStateException("falha do ciclo")).when(job).executar();

        assertThatThrownBy(scheduler::executarReconciliacaoNocturna)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha do ciclo");
        verify(job).executar();
    }

    @Test
    void deveManterAnotacaoDeAgendamentoSomenteNoScheduler() throws NoSuchMethodException {
        Method metodoScheduler = ReconciliacaoBarcodeScheduler.class
                .getDeclaredMethod("executarReconciliacaoNocturna");
        Method metodoJob = ReconciliacaoBarcodeJob.class.getDeclaredMethod("executar");

        Scheduled scheduled = metodoScheduler.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${gate.reconciliacao.cron:0 0 2 * * *}");
        assertThat(metodoJob.getAnnotation(Scheduled.class)).isNull();
    }
}
