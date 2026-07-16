package br.com.cloudport.monolitonavio.controlador;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfiguracaoFrontendControlador {

    private final String baseApiUrl;
    private final List<String> trustedParentOrigins;

    public ConfiguracaoFrontendControlador(
            @Value("${CONTROL_ROOM_BASE_API_URL:}") String baseApiUrl,
            @Value("${CONTROL_ROOM_TRUSTED_PARENT_ORIGINS:http://localhost:4200}") String trustedParentOrigins) {
        this.baseApiUrl = baseApiUrl == null ? "" : baseApiUrl.trim();
        this.trustedParentOrigins = separarOrigens(trustedParentOrigins);
    }

    @GetMapping(value = "/assets/configuracao.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ConfiguracaoFrontendResposta obterConfiguracao() {
        return new ConfiguracaoFrontendResposta(baseApiUrl, trustedParentOrigins);
    }

    private List<String> separarOrigens(String origins) {
        if (!StringUtils.hasText(origins)) {
            return Collections.singletonList("http://localhost:4200");
        }
        List<String> resultado = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        return resultado.isEmpty()
                ? Collections.singletonList("http://localhost:4200")
                : Collections.unmodifiableList(resultado);
    }

    public static final class ConfiguracaoFrontendResposta {
        private final String baseApiUrl;
        private final List<String> trustedParentOrigins;

        public ConfiguracaoFrontendResposta(String baseApiUrl, List<String> trustedParentOrigins) {
            this.baseApiUrl = baseApiUrl;
            this.trustedParentOrigins = trustedParentOrigins;
        }

        public String getBaseApiUrl() {
            return baseApiUrl;
        }

        public List<String> getTrustedParentOrigins() {
            return trustedParentOrigins;
        }
    }
}
