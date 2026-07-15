package br.com.cloudport.servicoyard.configuracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_SERVICE_KEY = "X-CloudPort-Service-Key";
    private static final String PRINCIPAL = "servico-navio-siderurgico";

    private final String serviceKey;

    public InternalServiceAuthenticationFilter(
            @Value("${cloudport.security.internal-service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null && chaveValida(request)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    PRINCIPAL,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE_NAVIO"))
            );
            authentication.setDetails(request.getRemoteAddr());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean chaveValida(HttpServletRequest request) {
        String recebida = request.getHeader(HEADER_SERVICE_KEY);
        if (!StringUtils.hasText(serviceKey) || !StringUtils.hasText(recebida)) {
            return false;
        }
        return MessageDigest.isEqual(
                serviceKey.getBytes(StandardCharsets.UTF_8),
                recebida.getBytes(StandardCharsets.UTF_8)
        );
    }
}
