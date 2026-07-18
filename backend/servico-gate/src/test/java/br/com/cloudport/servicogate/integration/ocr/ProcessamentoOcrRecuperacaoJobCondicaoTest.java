package br.com.cloudport.servicogate.integration.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class ProcessamentoOcrRecuperacaoJobCondicaoTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withBean(
                    DocumentoAgendamentoRepository.class,
                    () -> mock(DocumentoAgendamentoRepository.class))
            .withBean(ProcessamentoOcrPublisher.class, () -> mock(ProcessamentoOcrPublisher.class))
            .withUserConfiguration(ProcessamentoOcrRecuperacaoJob.class);

    @Test
    void naoDeveRegistrarJobQuandoFlagNaoFoiConfigurada() {
        contextRunner.run(contexto -> assertThat(contexto)
                .doesNotHaveBean(ProcessamentoOcrRecuperacaoJob.class));
    }

    @Test
    void naoDeveRegistrarJobQuandoFlagEstaDesabilitada() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=false")
                .run(contexto -> assertThat(contexto)
                        .doesNotHaveBean(ProcessamentoOcrRecuperacaoJob.class));
    }

    @Test
    void deveRegistrarJobSomenteQuandoFlagEstaHabilitada() {
        contextRunner
                .withPropertyValues("cloudport.runtime.jobs-enabled=true")
                .run(contexto -> assertThat(contexto)
                        .hasSingleBean(ProcessamentoOcrRecuperacaoJob.class));
    }

    @Test
    void deveManterJobsDesabilitadosPorPadraoNoServicoGate() throws IOException {
        Properties propriedades = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));

        assertThat(propriedades.getProperty("cloudport.runtime.jobs-enabled"))
                .isEqualTo("${CLOUDPORT_JOBS_ENABLED:false}");
    }
}
