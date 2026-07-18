package br.com.cloudport.servicoyard.configuracao;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguracao implements WebSocketMessageBrokerConfigurer {

    private static final String ORIGEM_LOCAL_PADRAO = "http://localhost:4200";

    private final String[] origensPermitidas;
    private final InterceptadorHandshakeWebSocket interceptadorHandshake;
    private final InterceptadorAutorizacaoWebSocket interceptadorAutorizacao;

    public WebSocketConfiguracao(
            @Value("${cloudport.security.cors.allowed-origins:http://localhost:4200}") String allowedOrigins,
            InterceptadorHandshakeWebSocket interceptadorHandshake,
            InterceptadorAutorizacaoWebSocket interceptadorAutorizacao) {
        this.origensPermitidas = carregarOrigensPermitidas(allowedOrigins);
        this.interceptadorHandshake = interceptadorHandshake;
        this.interceptadorAutorizacao = interceptadorAutorizacao;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topico");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptadorAutorizacao);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registrarEndpoint(registry, CanalWebSocketOperacional.PATIO);
        registrarEndpoint(registry, CanalWebSocketOperacional.RECURSOS);
        registrarEndpoint(registry, CanalWebSocketOperacional.EDI);
    }

    private void registrarEndpoint(StompEndpointRegistry registry, CanalWebSocketOperacional canal) {
        registry.addEndpoint(canal.getEndpoint())
                .setAllowedOrigins(origensPermitidas)
                .addInterceptors(interceptadorHandshake);
    }

    private String[] carregarOrigensPermitidas(String allowedOrigins) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(origin -> !"*".equals(origin))
                .distinct()
                .toArray(String[]::new);
        if (origins.length == 0) {
            return new String[]{ORIGEM_LOCAL_PADRAO};
        }
        return origins;
    }
}
