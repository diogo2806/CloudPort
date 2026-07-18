package br.com.cloudport.servicoyard.configuracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;

class InterceptadorHandshakeWebSocketTest {

    private final InterceptadorHandshakeWebSocket interceptador = new InterceptadorHandshakeWebSocket();

    @Test
    void deveRejeitarHandshakeAnonimo() {
        ServerHttpRequest request = request("/ws/patio", null);

        boolean permitido = interceptador.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                new HashMap<>());

        assertThat(permitido).isFalse();
    }

    @Test
    void deveRejeitarPerfilSemPermissaoParaRecursos() {
        Authentication authentication = autenticacao("OPERADOR_GATE");
        ServerHttpRequest request = request("/ws/recursos", authentication);

        boolean permitido = interceptador.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                new HashMap<>());

        assertThat(permitido).isFalse();
    }

    @Test
    void deveRegistrarCanalEPrincipalNoHandshakeAutorizado() {
        Authentication authentication = autenticacao("OPERADOR_PATIO");
        ServerHttpRequest request = request("/ws/recursos", authentication);
        Map<String, Object> atributos = new HashMap<>();

        boolean permitido = interceptador.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                atributos);

        assertThat(permitido).isTrue();
        assertThat(atributos)
                .containsEntry(InterceptadorHandshakeWebSocket.ATRIBUTO_CANAL,
                        CanalWebSocketOperacional.RECURSOS.name())
                .containsEntry(InterceptadorHandshakeWebSocket.ATRIBUTO_PRINCIPAL, authentication);
    }

    @Test
    void deveRejeitarEndpointNaoDeclarado() {
        Authentication authentication = autenticacao("ADMIN_PORTO");
        ServerHttpRequest request = request("/ws/desconhecido", authentication);

        boolean permitido = interceptador.beforeHandshake(
                request,
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                new HashMap<>());

        assertThat(permitido).isFalse();
    }

    private static ServerHttpRequest request(String path, Authentication authentication) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost" + path));
        when(request.getPrincipal()).thenReturn(authentication);
        return request;
    }

    private static Authentication autenticacao(String role) {
        return new UsernamePasswordAuthenticationToken(
                "usuario",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
