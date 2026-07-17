package br.com.cloudport.visibilidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class VisibilidadeDashboardJobTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(VisibilidadeDashboardService.class,
                    () -> mock(VisibilidadeDashboardService.class))
            .withUserConfiguration(VisibilidadeDashboardJob.class);

    @Test
    void naoDeveCriarJobQuandoAgendamentosEstaoDesabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> assertThat(contexto)
                        .doesNotHaveBean(VisibilidadeDashboardJob.class));
    }

    @Test
    void deveCriarJobQuandoAgendamentosEstaoHabilitados() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> assertThat(contexto)
                        .hasSingleBean(VisibilidadeDashboardJob.class));
    }

    @Test
    void deveCriarJobPorPadraoParaPreservarExecucaoStandalone() {
        contextRunner.run(contexto -> assertThat(contexto)
                .hasSingleBean(VisibilidadeDashboardJob.class));
    }

    @Test
    void deveDelegarPublicacaoEDeteccaoAoServico() {
        VisibilidadeDashboardService service = mock(VisibilidadeDashboardService.class);
        VisibilidadeDashboardJob job = new VisibilidadeDashboardJob(service);

        job.publicarDashboard();
        job.detectarAlertasAutomaticos();

        verify(service).publicarDashboard();
        verify(service).detectarAlertasAutomaticos();
    }

    @Test
    void deveConcentrarAnotacoesDeAgendamentoNoJob() throws NoSuchMethodException {
        Method publicarJob = VisibilidadeDashboardJob.class.getDeclaredMethod("publicarDashboard");
        Method detectarJob = VisibilidadeDashboardJob.class.getDeclaredMethod("detectarAlertasAutomaticos");
        Method publicarService = VisibilidadeDashboardService.class.getDeclaredMethod("publicarDashboard");
        Method detectarService = VisibilidadeDashboardService.class.getDeclaredMethod("detectarAlertasAutomaticos");

        Scheduled publicarScheduled = publicarJob.getAnnotation(Scheduled.class);
        Scheduled detectarScheduled = detectarJob.getAnnotation(Scheduled.class);

        assertThat(publicarScheduled).isNotNull();
        assertThat(publicarScheduled.fixedDelayString())
                .isEqualTo("${visibilidade.dashboard.refresh-ms:30000}");
        assertThat(detectarScheduled).isNotNull();
        assertThat(detectarScheduled.fixedDelayString())
                .isEqualTo("${visibilidade.alertas.refresh-ms:60000}");
        assertThat(publicarService.getAnnotation(Scheduled.class)).isNull();
        assertThat(detectarService.getAnnotation(Scheduled.class)).isNull();
    }
}
