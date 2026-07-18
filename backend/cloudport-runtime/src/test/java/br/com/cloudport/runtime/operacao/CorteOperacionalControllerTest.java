package br.com.cloudport.runtime.operacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.runtime.operacao.CorteOperacionalController.EstadoCorteOperacional;
import org.junit.jupiter.api.Test;

class CorteOperacionalControllerTest {

    @Test
    void deveIdentificarInstanciaCanonicaComAdaptadoresLocais() {
        CorteOperacionalController controller = new CorteOperacionalController(
                "runtime-01",
                true,
                true,
                true,
                "local",
                "local",
                "local",
                "abc123");

        EstadoCorteOperacional estado = controller.consultar();

        assertThat(estado.runtime()).isEqualTo("cloudport-runtime");
        assertThat(estado.instanciaId()).isEqualTo("runtime-01");
        assertThat(estado.papel()).isEqualTo("CANONICO_ATIVO");
        assertThat(estado.adaptadoresLocais()).isTrue();
        assertThat(estado.schemas()).hasSize(8);
        assertThat(estado.integracoes()).containsOnlyKeys("autenticacao", "navio", "yard");
    }

    @Test
    void deveIdentificarObservadorSemEscritaJobsOuConsumidores() {
        CorteOperacionalController controller = new CorteOperacionalController(
                "observer-01",
                false,
                false,
                false,
                "local",
                "local",
                "local",
                "abc123");

        EstadoCorteOperacional estado = controller.consultar();

        assertThat(estado.papel()).isEqualTo("OBSERVACAO_SOMENTE_LEITURA");
        assertThat(estado.escritaHabilitada()).isFalse();
        assertThat(estado.jobsHabilitados()).isFalse();
        assertThat(estado.consumidoresHabilitados()).isFalse();
    }
}
