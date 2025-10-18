package br.com.cloudport.servicoautenticacao.app.privacidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoautenticacao.app.privacidade.dto.OpcaoPrivacidadeRespostaDTO;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PrivacidadeConsultaServicoTest {

    @Mock
    private ConfiguracaoPrivacidadeRepositorio configuracaoPrivacidadeRepositorio;

    @Mock
    private SanitizadorConteudoPrivacidade sanitizadorConteudoPrivacidade;

    @InjectMocks
    private PrivacidadeConsultaServico privacidadeConsultaServico;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deveSanitizarDescricoesAntesDeRetornarOpcoes() {
        ConfiguracaoPrivacidade configuracao = new ConfiguracaoPrivacidade();
        configuracao.setId(UUID.randomUUID());
        configuracao.setDescricao("<script>alert('xss')</script><b>Texto Seguro</b>");
        configuracao.setAtivo(true);

        when(configuracaoPrivacidadeRepositorio.findAll()).thenReturn(List.of(configuracao));
        when(sanitizadorConteudoPrivacidade.sanitizarDescricao(configuracao.getDescricao())).thenReturn("Texto Seguro");

        List<OpcaoPrivacidadeRespostaDTO> resposta = privacidadeConsultaServico.listarOpcoes();

        assertThat(resposta)
                .hasSize(1)
                .allSatisfy(opcao -> {
                    assertThat(opcao.getDescricao()).isEqualTo("Texto Seguro");
                    assertThat(opcao.isAtivo()).isTrue();
                });
    }
}
