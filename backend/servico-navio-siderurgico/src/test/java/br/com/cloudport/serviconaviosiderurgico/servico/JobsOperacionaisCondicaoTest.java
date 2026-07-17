package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JobsOperacionaisCondicaoTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ReservaPatioNavioServico.class, () -> mock(ReservaPatioNavioServico.class))
            .withBean(ExecucaoUnicaServico.class, () -> mock(ExecucaoUnicaServico.class))
            .withBean(ItemOperacaoNavioRepositorio.class, () -> mock(ItemOperacaoNavioRepositorio.class))
            .withBean(SincronizadorStatusNavioPatioServico.class,
                    () -> mock(SincronizadorStatusNavioPatioServico.class))
            .withBean(NavioSiderurgicoServico.class, () -> mock(NavioSiderurgicoServico.class))
            .withUserConfiguration(
                    ExpiracaoReservaPatioJob.class,
                    ReconciliacaoNavioPatioJob.class,
                    SincronizacaoCadastroCanonicoJob.class);

    @Test
    void naoDeveRegistrarJobsQuandoFlagNaoFoiConfigurada() {
        contextRunner.run(contexto -> {
            assertThat(contexto).doesNotHaveBean(ExpiracaoReservaPatioJob.class);
            assertThat(contexto).doesNotHaveBean(ReconciliacaoNavioPatioJob.class);
            assertThat(contexto).doesNotHaveBean(SincronizacaoCadastroCanonicoJob.class);
        });
    }

    @Test
    void naoDeveRegistrarJobsQuandoFlagEstaDesabilitada() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> {
                    assertThat(contexto).doesNotHaveBean(ExpiracaoReservaPatioJob.class);
                    assertThat(contexto).doesNotHaveBean(ReconciliacaoNavioPatioJob.class);
                    assertThat(contexto).doesNotHaveBean(SincronizacaoCadastroCanonicoJob.class);
                });
    }

    @Test
    void deveRegistrarJobsSomenteQuandoFlagEstaHabilitada() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> {
                    assertThat(contexto).hasSingleBean(ExpiracaoReservaPatioJob.class);
                    assertThat(contexto).hasSingleBean(ReconciliacaoNavioPatioJob.class);
                    assertThat(contexto).hasSingleBean(SincronizacaoCadastroCanonicoJob.class);
                });
    }
}
