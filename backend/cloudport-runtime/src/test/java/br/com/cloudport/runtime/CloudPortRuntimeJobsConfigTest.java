package br.com.cloudport.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class CloudPortRuntimeJobsConfigTest {

    @Test
    void deveManterJobsDesabilitadosPorPadraoNoRuntimeConsolidado() throws IOException {
        Properties propriedades = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));

        assertThat(propriedades.getProperty("cloudport.runtime.jobs-enabled"))
                .isEqualTo("${CLOUDPORT_JOBS_ENABLED:false}");
    }
}
