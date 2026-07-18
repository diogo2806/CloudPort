package br.com.cloudport.servicoyard.configuracao;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class InterceptadorHandshakeWebSocket implements HandshakeInterceptor {

    public static final String ATRIBUTO_CANAL = "cloudport.websocket.canal";
    public static final String ATRIBUTO_PRINCIPAL = "cloudport.websocket.principal";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        Optional<CanalWebSocketOperacional> canal = CanalWebSocketOperacional
                .identificarPorCaminho(request.getURI().getPath());
        Principal principal = request.getPrincipal();

        if (canal.isEmpty() || !autenticacaoValida(principal)) {
            return false;
        }

        Authentication authentication = (Authentication) principal;
        if (!canal.get().permite(authentication)) {
            return false;
        }

        attributes.put(ATRIBUTO_CANAL, canal.get().name());
        attributes.put(ATRIBUTO_PRINCIPAL, authentication);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Não há estado adicional para liberar após o handshake.
    }

    private boolean autenticacaoValida(Principal principal) {
        if (!(principal instanceof Authentication)) {
            return false;
        }
        Authentication authentication = (Authentication) principal;
        return authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
