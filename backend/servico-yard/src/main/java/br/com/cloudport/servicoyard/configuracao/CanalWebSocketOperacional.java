package br.com.cloudport.servicoyard.configuracao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

public enum CanalWebSocketOperacional {

    PATIO(
            "/ws/patio",
            "/topico/patio",
            Set.of(
                    "ROLE_ADMIN_PORTO",
                    "ROLE_PLANEJADOR",
                    "ROLE_OPERADOR_PATIO",
                    "ROLE_OPERADOR_GATE",
                    "ROLE_SERVICE_NAVIO",
                    "ROLE_SERVICE_SIDERURGICO"
            )
    ),
    RECURSOS(
            "/ws/recursos",
            "/topico/recursos",
            Set.of(
                    "ROLE_ADMIN_PORTO",
                    "ROLE_PLANEJADOR",
                    "ROLE_OPERADOR_PATIO"
            )
    ),
    EDI(
            "/ws/edi",
            "/topico/edi/bay-plan",
            Set.of(
                    "ROLE_ADMIN_PORTO",
                    "ROLE_PLANEJADOR",
                    "ROLE_SERVICE_NAVIO",
                    "ROLE_SERVICE_SIDERURGICO"
            )
    );

    private final String endpoint;
    private final String destinoPermitido;
    private final Set<String> autoridadesPermitidas;

    CanalWebSocketOperacional(String endpoint,
                              String destinoPermitido,
                              Set<String> autoridadesPermitidas) {
        this.endpoint = endpoint;
        this.destinoPermitido = destinoPermitido;
        this.autoridadesPermitidas = autoridadesPermitidas;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean correspondeAoCaminho(String caminho) {
        if (!StringUtils.hasText(caminho)) {
            return false;
        }
        return caminho.equals(endpoint)
                || caminho.endsWith(endpoint)
                || caminho.contains(endpoint + "/");
    }

    public boolean permiteDestino(String destino) {
        if (!StringUtils.hasText(destino)) {
            return false;
        }
        return destino.equals(destinoPermitido) || destino.startsWith(destinoPermitido + "/");
    }

    public boolean permite(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridadesPermitidas::contains);
    }

    public static Optional<CanalWebSocketOperacional> identificarPorCaminho(String caminho) {
        return Arrays.stream(values())
                .filter(canal -> canal.correspondeAoCaminho(caminho))
                .findFirst();
    }

    public static Optional<CanalWebSocketOperacional> identificarPorDestino(String destino) {
        return Arrays.stream(values())
                .filter(canal -> canal.permiteDestino(destino))
                .findFirst();
    }
}
