package br.com.cloudport.servicoyard.configuracao;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class InterceptadorAutorizacaoWebSocket implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        StompCommand command = accessor.getCommand();
        if (command == null || command == StompCommand.DISCONNECT || command == StompCommand.UNSUBSCRIBE) {
            return message;
        }

        Optional<CanalWebSocketOperacional> canal = obterCanal(accessor);
        String destino = accessor.getDestination();

        if (command == StompCommand.CONNECT) {
            canal.ifPresent(canalOperacional -> validarConexao(obterAutenticacao(accessor), canalOperacional));
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            if (canal.isPresent()) {
                Authentication authentication = obterAutenticacao(accessor);
                validarConexao(authentication, canal.get());
                validarAssinatura(authentication, canal.get(), destino);
                return message;
            }
            if (CanalWebSocketOperacional.identificarPorDestino(destino).isPresent()) {
                throw new AccessDeniedException("Assinatura WebSocket não autorizada para o destino solicitado.");
            }
            return message;
        }

        if (command == StompCommand.SEND) {
            if (canal.isPresent() || CanalWebSocketOperacional.identificarPorDestino(destino).isPresent()) {
                throw new AccessDeniedException("Clientes WebSocket não podem publicar em tópicos operacionais.");
            }
            return message;
        }

        return message;
    }

    private Authentication obterAutenticacao(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            Map<String, Object> atributos = accessor.getSessionAttributes();
            if (atributos != null) {
                Object principalSessao = atributos.get(InterceptadorHandshakeWebSocket.ATRIBUTO_PRINCIPAL);
                if (principalSessao instanceof Principal) {
                    principal = (Principal) principalSessao;
                }
            }
        }

        if (!(principal instanceof Authentication)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Autenticação obrigatória para o canal WebSocket.");
        }

        Authentication authentication = (Authentication) principal;
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Autenticação obrigatória para o canal WebSocket.");
        }
        return authentication;
    }

    private Optional<CanalWebSocketOperacional> obterCanal(StompHeaderAccessor accessor) {
        Map<String, Object> atributos = accessor.getSessionAttributes();
        if (atributos == null) {
            return Optional.empty();
        }

        Object valor = atributos.get(InterceptadorHandshakeWebSocket.ATRIBUTO_CANAL);
        if (valor == null) {
            return Optional.empty();
        }
        if (!(valor instanceof String)) {
            throw new AccessDeniedException("Canal WebSocket operacional inválido.");
        }

        try {
            return Optional.of(CanalWebSocketOperacional.valueOf((String) valor));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Canal WebSocket operacional inválido.", exception);
        }
    }

    private void validarConexao(Authentication authentication, CanalWebSocketOperacional canal) {
        if (!canal.permite(authentication)) {
            throw new AccessDeniedException("Perfil sem permissão para o canal WebSocket solicitado.");
        }
    }

    private void validarAssinatura(Authentication authentication,
                                   CanalWebSocketOperacional canal,
                                   String destino) {
        if (!canal.permiteDestino(destino) || !canal.permite(authentication)) {
            throw new AccessDeniedException("Assinatura WebSocket não autorizada para o destino solicitado.");
        }
    }
}
