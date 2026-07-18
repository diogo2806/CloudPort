package br.com.cloudport.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class CloudPortRuntimeJobsConfigTest {

    @Test
    void deveManterEscritasJobsEConsumidoresDesabilitadosPorPadrao() throws IOException {
        Properties propriedades = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));

        assertThat(propriedades.getProperty("cloudport.runtime.writes-enabled"))
                .isEqualTo("${CLOUDPORT_WRITES_ENABLED:false}");
        assertThat(propriedades.getProperty("cloudport.runtime.jobs-enabled"))
                .isEqualTo("${CLOUDPORT_JOBS_ENABLED:false}");
        assertThat(propriedades.getProperty("cloudport.runtime.consumers-enabled"))
                .isEqualTo("${CLOUDPORT_CONSUMERS_ENABLED:false}");
    }
}
