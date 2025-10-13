package br.com.cloudport.servicogate.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TransportadoraSynchronizationFilter extends OncePerRequestFilter {

    private final TransportadoraSyncService transportadoraSyncService;
    private final AutenticacaoClient autenticacaoClient;

    public TransportadoraSynchronizationFilter(TransportadoraSyncService transportadoraSyncService,
                                               AutenticacaoClient autenticacaoClient) {
        this.transportadoraSyncService = transportadoraSyncService;
        this.autenticacaoClient = autenticacaoClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
            Jwt token = jwtAuthentication.getToken();
            String documento = token.getClaimAsString("transportadoraDocumento");
            if (!StringUtils.hasText(documento)) {
                documento = token.getClaimAsString("transportadoraCnpj");
            }
            String nome = token.getClaimAsString("transportadoraNome");

            if (!StringUtils.hasText(documento) || !StringUtils.hasText(nome)) {
                var info = autenticacaoClient.buscarUsuario(token.getSubject(), request.getHeader(HttpHeaders.AUTHORIZATION));
                if (!StringUtils.hasText(documento)) {
                    documento = info.map(UserInfoResponse::getTransportadoraDocumento).orElse(null);
                }
                if (!StringUtils.hasText(nome)) {
                    nome = info.map(UserInfoResponse::getTransportadoraNome).orElse(null);
                }
            }

            if (StringUtils.hasText(documento)) {
                transportadoraSyncService.sincronizarTransportadora(documento, nome);
            }
        }
        filterChain.doFilter(request, response);
    }
}
