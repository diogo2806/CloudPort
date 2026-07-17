package br.com.cloudport.serviconaviosiderurgico.configuracao;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class CredenciaisSegurancaValidator {

    static final int SEGREDO_JWT_MINIMO_BYTES = 32;
    static final int SEGREDO_CLIENTE_PUBLICO_MINIMO_BYTES = 32;

    private static final Set<String> SEGREDOS_SENTINELA = Set.of(
            "chave-local-para-desenvolvimento-123456",
            "troque-esta-chave-publica"
    );
    private static final Set<String> CLIENTES_SENTINELA = Set.of("cloudport-local");

    private CredenciaisSegurancaValidator() {
    }

    public static String validarSegredoJwt(String segredo) {
        return validarSegredo(
                segredo,
                SEGREDO_JWT_MINIMO_BYTES,
                "cloudport.security.jwt.secret deve ser configurado externamente com ao menos 256 bits (32 bytes)."
        );
    }

    public static Map<String, String> carregarClientesPublicos(String configuracao) {
        if (!StringUtils.hasText(configuracao)) {
            throw new IllegalStateException(
                    "cloudport.security.public-api.clients deve ser configurado externamente."
            );
        }

        Map<String, String> resultado = new LinkedHashMap<>();
        Arrays.stream(configuracao.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(entrada -> adicionarCliente(resultado, entrada));

        if (resultado.isEmpty()) {
            throw new IllegalStateException(
                    "cloudport.security.public-api.clients deve conter ao menos um cliente valido."
            );
        }
        return Map.copyOf(resultado);
    }

    private static void adicionarCliente(Map<String, String> resultado, String entrada) {
        int separador = entrada.indexOf(':');
        if (separador <= 0 || separador == entrada.length() - 1) {
            throw new IllegalStateException(
                    "cloudport.security.public-api.clients deve usar o formato cliente:segredo."
            );
        }

        String id = entrada.substring(0, separador).trim();
        String segredo = entrada.substring(separador + 1).trim();
        if (!StringUtils.hasText(id)) {
            throw new IllegalStateException("Identificador de cliente publico invalido.");
        }
        if (CLIENTES_SENTINELA.contains(id.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Cliente publico sentinela de desenvolvimento nao pode ser utilizado.");
        }

        String segredoValidado = validarSegredo(
                segredo,
                SEGREDO_CLIENTE_PUBLICO_MINIMO_BYTES,
                "O segredo de cada cliente publico deve ter ao menos 256 bits (32 bytes)."
        );
        if (resultado.putIfAbsent(id, segredoValidado) != null) {
            throw new IllegalStateException("Cliente publico duplicado: " + id);
        }
    }

    private static String validarSegredo(String segredo, int minimoBytes, String mensagemTamanho) {
        if (!StringUtils.hasText(segredo)) {
            throw new IllegalStateException(mensagemTamanho);
        }

        String valor = segredo.trim();
        if (SEGREDOS_SENTINELA.contains(valor.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Segredo sentinela de desenvolvimento nao pode ser utilizado.");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length < minimoBytes) {
            throw new IllegalStateException(mensagemTamanho);
        }
        return valor;
    }
}
