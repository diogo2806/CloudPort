package br.com.cloudport.servicocargageral.configuracao;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class SegredoJwtValidator {

    static final int SEGREDO_JWT_MINIMO_BYTES = 32;

    private static final Set<String> SEGREDOS_SENTINELA = Set.of(
            "chave-local-para-desenvolvimento-123456",
            "troque-esta-chave-publica",
            "changeit",
            "changeme"
    );

    private SegredoJwtValidator() {
    }

    public static String validar(String segredo) {
        if (!StringUtils.hasText(segredo)) {
            throw new IllegalStateException(
                    "cloudport.security.jwt.secret deve ser configurado externamente com ao menos 256 bits (32 bytes)."
            );
        }

        String valor = segredo.trim();
        if (SEGREDOS_SENTINELA.contains(valor.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Segredo sentinela de desenvolvimento nao pode ser utilizado.");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length < SEGREDO_JWT_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "cloudport.security.jwt.secret deve ser configurado externamente com ao menos 256 bits (32 bytes)."
            );
        }
        return valor;
    }
}
