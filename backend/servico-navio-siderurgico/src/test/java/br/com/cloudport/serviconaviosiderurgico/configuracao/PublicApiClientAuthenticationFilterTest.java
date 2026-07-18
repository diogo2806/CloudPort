package br.com.cloudport.serviconaviosiderurgico.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PublicApiClientAuthenticationFilterTest {

    private static final String CLIENT_ID = "secretaria-portos";
    private static final String CLIENT_SECRET = "segredo-publico-com-32-caracteres";
    private static final String CAMINHO_PUBLICO = "/api/public/v1/secretaria-clientes:sync";

    private final PublicApiClientAuthenticationFilter filter = new PublicApiClientAuthenticationFilter(
            CLIENT_ID + ":" + CLIENT_SECRET,
            new ObjectMapper().findAndRegisterModules()
    );

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarClienteValidoComRoleExternaELimparContextoAoFinal() throws Exception {
        MockHttpServletRequest request = requisicaoPublica();
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, CLIENT_ID);
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET, CLIENT_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> autenticacaoDuranteRequisicao = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) ->
                autenticacaoDuranteRequisicao.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(autenticacaoDuranteRequisicao.get()).isNotNull();
        assertThat(autenticacaoDuranteRequisicao.get().getName()).isEqualTo("client:" + CLIENT_ID);
        assertThat(autenticacaoDuranteRequisicao.get().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_INTEGRACAO_EXTERNA");
        assertThat(request.getAttribute(PublicApiClientAuthenticationFilter.ATRIBUTO_CLIENT_ID))
                .isEqualTo(CLIENT_ID);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveRejeitarCredenciaisInvalidasSemReaproveitarAutenticacaoAnterior() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("usuario-anterior", null, "ROLE_ADMIN")
        );
        MockHttpServletRequest request = requisicaoPublica();
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, CLIENT_ID);
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET, "segredo-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecutada = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainExecutada.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("CLIENTE_PUBLICO_INVALIDO");
        assertThat(chainExecutada).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveInicializarSemClientesConfiguradosEManterRotaPublicaBloqueada() throws Exception {
        PublicApiClientAuthenticationFilter filterSemClientes = new PublicApiClientAuthenticationFilter(
                " ",
                new ObjectMapper().findAndRegisterModules()
        );
        MockHttpServletRequest request = requisicaoPublica();
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, CLIENT_ID);
        request.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET, CLIENT_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecutada = new AtomicBoolean(false);

        filterSemClientes.doFilter(request, response,
                (servletRequest, servletResponse) -> chainExecutada.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("CLIENTE_PUBLICO_INVALIDO");
        assertThat(chainExecutada).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveIsolarRequisicoesSequenciais() throws Exception {
        MockHttpServletRequest primeiraRequisicao = requisicaoPublica();
        primeiraRequisicao.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID, CLIENT_ID);
        primeiraRequisicao.addHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET, CLIENT_SECRET);
        MockHttpServletResponse primeiraResposta = new MockHttpServletResponse();
        AtomicReference<Authentication> primeiraAutenticacao = new AtomicReference<>();

        filter.doFilter(primeiraRequisicao, primeiraResposta, (servletRequest, servletResponse) ->
                primeiraAutenticacao.set(SecurityContextHolder.getContext().getAuthentication()));

        MockHttpServletRequest segundaRequisicao = requisicaoPublica();
        MockHttpServletResponse segundaResposta = new MockHttpServletResponse();
        AtomicBoolean segundaChainExecutada = new AtomicBoolean(false);

        filter.doFilter(segundaRequisicao, segundaResposta,
                (servletRequest, servletResponse) -> segundaChainExecutada.set(true));

        assertThat(primeiraAutenticacao.get()).isNotNull();
        assertThat(primeiraResposta.getStatus()).isEqualTo(200);
        assertThat(segundaResposta.getStatus()).isEqualTo(401);
        assertThat(segundaChainExecutada).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveIgnorarPreflightCorsSemExigirCredenciais() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", CAMINHO_PUBLICO);
        request.addHeader("Origin", "http://localhost:4200");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecutada = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainExecutada.set(true));

        assertThat(chainExecutada).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requisicaoPublica() {
        return new MockHttpServletRequest("POST", CAMINHO_PUBLICO);
    }
}
