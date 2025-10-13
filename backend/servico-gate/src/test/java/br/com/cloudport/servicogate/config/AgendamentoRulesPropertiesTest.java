package br.com.cloudport.servicogate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgendamentoRulesPropertiesTest {

    @Test
    @DisplayName("Deve expor valores padrão coerentes para as regras de agendamento")
    void deveAplicarValoresPadrao() {
        AgendamentoRulesProperties properties = new AgendamentoRulesProperties();

        assertThat(properties.getAntecedenciaMinima()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.getAtrasoMaximo()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.getEdicaoAntecedencia()).isEqualTo(Duration.ofHours(12));
        assertThat(properties.getEdicaoAtraso()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.getNotificacaoJanelaAntecedencia()).isEqualTo(Duration.ofMinutes(30));
    }
}

