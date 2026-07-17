package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.com.cloudport.serviconavio.configuracao.InternalServiceAuthenticationFilter;
import br.com.cloudport.serviconaviosiderurgico.configuracao.PublicApiClientAuthenticationFilter;
import org.junit.jupiter.api.Test;

class ConfiguracaoSegurancaRuntimeCredenciaisTest {

    @Test
    void deveRejeitarSegredoJwtSentinelaNoRuntime() {
        ConfiguracaoSegurancaRuntime configuracao = new ConfiguracaoSegurancaRuntime(
                "chave-local-para-desenvolvimento-123456",
                "http://localhost:4200",
                mock(InternalServiceAuthenticationFilter.class),
                mock(PublicApiClientAuthenticationFilter.class)
        );

        assertThatThrownBy(configuracao::jwtDecoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinela");
    }
}
