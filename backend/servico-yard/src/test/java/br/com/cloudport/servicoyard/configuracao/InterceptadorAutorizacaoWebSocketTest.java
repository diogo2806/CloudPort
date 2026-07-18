package br.com.cloudport.servicoyard.configuracao;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class InterceptadorAutorizacaoWebSocketTest {

    private final InterceptadorAutorizacaoWebSocket interceptador = new InterceptadorAutorizacaoWebSocket();
    private final MessageChannel canalMensagens = message -> true;

    @Test
    void deveRejeitarConnectAnonimo() {
        Message<byte[]> mensagem = mensagem(
                StompCommand.CONNECT,
                CanalWebSocketOperacional.PATIO,
                null,
                null);

        assertThatThrownBy(() -> interceptador.preSend(mensagem, canalMensagens))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void devePermitirAssinaturaDoPatioParaOperadorGate() {
        Authentication authentication = autenticacao("OPERADOR_GATE");
        Message<byte[]> mensagem = mensagem(
                StompCommand.SUBSCRIBE,
                CanalWebSocketOperacional.PATIO,
                authentication,
                "/topico/patio");

        assertThatCode(() -> interceptador.preSend(mensagem, canalMensagens))
                .doesNotThrowAnyException();
    }

    @Test
    void deveBloquearAssinaturaCruzadaEntreCanais() {
        Authentication authentication = autenticacao("ADMIN_PORTO");
        Message<byte[]> mensagem = mensagem(
                StompCommand.SUBSCRIBE,
                CanalWebSocketOperacional.PATIO,
                authentication,
                "/topico/recursos");

        assertThatThrownBy(() -> interceptador.preSend(mensagem, canalMensagens))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveBloquearRecursosParaOperadorGate() {
        Authentication authentication = autenticacao("OPERADOR_GATE");
        Message<byte[]> mensagem = mensagem(
                StompCommand.CONNECT,
                CanalWebSocketOperacional.RECURSOS,
                authentication,
                null);

        assertThatThrownBy(() -> interceptador.preSend(mensagem, canalMensagens))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void devePermitirRecursosParaOperadorPatio() {
        Authentication authentication = autenticacao("OPERADOR_PATIO");
        Message<byte[]> mensagem = mensagem(
                StompCommand.SUBSCRIBE,
                CanalWebSocketOperacional.RECURSOS,
                authentication,
                "/topico/recursos");

        assertThatCode(() -> interceptador.preSend(mensagem, canalMensagens))
                .doesNotThrowAnyException();
    }

    @Test
    void deveBloquearDestinoNaoDeclarado() {
        Authentication authentication = autenticacao("ADMIN_PORTO");
        Message<byte[]> mensagem = mensagem(
                StompCommand.SUBSCRIBE,
                CanalWebSocketOperacional.PATIO,
                authentication,
                "/topico/interno");

        assertThatThrownBy(() -> interceptador.preSend(mensagem, canalMensagens))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveBloquearPublicacaoDoCliente() {
        Authentication authentication = autenticacao("ADMIN_PORTO");
        Message<byte[]> mensagem = mensagem(
                StompCommand.SEND,
                CanalWebSocketOperacional.PATIO,
                authentication,
                "/topico/patio");

        assertThatThrownBy(() -> interceptador.preSend(mensagem, canalMensagens))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static Message<byte[]> mensagem(StompCommand command,
                                             CanalWebSocketOperacional canal,
                                             Authentication authentication,
                                             String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(InterceptadorHandshakeWebSocket.ATRIBUTO_CANAL, canal.name());
        if (authentication != null) {
            atributos.put(InterceptadorHandshakeWebSocket.ATRIBUTO_PRINCIPAL, authentication);
            accessor.setUser(authentication);
        }
        accessor.setSessionAttributes(atributos);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Authentication autenticacao(String role) {
        return new UsernamePasswordAuthenticationToken(
                "usuario",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
