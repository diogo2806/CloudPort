package br.com.cloudport.servicoyard.patio.listatrabalho.controlador;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class WorkQueuePatioControladorAutorizacaoTest {

    @Test
    void deveRestringirOperacoesAosPerfisOperacionaisEIntegracaoDeNavio() {
        PreAuthorize autorizacao = WorkQueuePatioControlador.class.getAnnotation(PreAuthorize.class);

        assertThat(autorizacao).isNotNull();
        assertThat(autorizacao.value())
                .contains("ADMIN_PORTO")
                .contains("PLANEJADOR")
                .contains("OPERADOR_GATE")
                .contains("SERVICE_NAVIO")
                .doesNotContain("permitAll")
                .doesNotContain("ANONYMOUS");
    }
}
