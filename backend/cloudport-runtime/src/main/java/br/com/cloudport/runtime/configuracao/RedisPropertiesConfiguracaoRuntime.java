package br.com.cloudport.runtime.configuracao;

import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedisPropertiesConfiguracaoRuntime {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RedisPropertiesConfiguracaoRuntime.class);

    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final int DEFAULT_REDIS_PORT = 6379;

    @Bean
    public static BeanPostProcessor redisPropertiesNormalizador() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RedisProperties) {
                    normalizar((RedisProperties) bean);
                }
                return bean;
            }
        };
    }

    static void normalizar(RedisProperties redisProperties) {
        normalizarUrl(redisProperties);

        String host = redisProperties.getHost();
        redisProperties.setHost(
                StringUtils.hasText(host) ? host.trim() : DEFAULT_REDIS_HOST);

        if (redisProperties.getPort() <= 0) {
            redisProperties.setPort(DEFAULT_REDIS_PORT);
        }
    }

    // O auto-configurador do Spring tem precedencia para spring.redis.url e a
    // interpreta com java.net.URI, que devolve host null para nomes com '_'
    // (padrao de containers do Docker Compose). Nesse caso a URL precisa ser
    // convertida em host/porta/credenciais discretos antes de criar o Lettuce.
    private static void normalizarUrl(RedisProperties redisProperties) {
        String url = redisProperties.getUrl();
        if (url == null) {
            return;
        }
        if (!StringUtils.hasText(url)) {
            redisProperties.setUrl(null);
            return;
        }

        String urlNormalizada = url.trim();
        URI uri;
        try {
            uri = new URI(urlNormalizada);
        } catch (URISyntaxException excecao) {
            LOGGER.warn(
                    "URL do Redis invalida ({}); usando host e porta configurados",
                    urlNormalizada);
            redisProperties.setUrl(null);
            return;
        }

        if (uri.getHost() != null) {
            redisProperties.setUrl(urlNormalizada);
            return;
        }

        if (!converterUrlParaCampos(redisProperties, uri)) {
            LOGGER.warn(
                    "Nao foi possivel extrair host da URL do Redis ({}); usando host e porta configurados",
                    urlNormalizada);
        }
        redisProperties.setUrl(null);
    }

    private static boolean converterUrlParaCampos(RedisProperties redisProperties, URI uri) {
        String autoridade = uri.getAuthority();
        if (!StringUtils.hasText(autoridade)) {
            return false;
        }

        String credenciais = null;
        String hostPorta = autoridade;
        int separadorUsuario = autoridade.lastIndexOf('@');
        if (separadorUsuario >= 0) {
            credenciais = autoridade.substring(0, separadorUsuario);
            hostPorta = autoridade.substring(separadorUsuario + 1);
        }

        String host = hostPorta;
        int porta = -1;
        int separadorPorta = hostPorta.lastIndexOf(':');
        if (separadorPorta >= 0) {
            host = hostPorta.substring(0, separadorPorta);
            try {
                porta = Integer.parseInt(hostPorta.substring(separadorPorta + 1));
            } catch (NumberFormatException excecao) {
                porta = -1;
            }
        }

        if (!StringUtils.hasText(host)) {
            return false;
        }

        redisProperties.setHost(host.trim());
        if (porta > 0) {
            redisProperties.setPort(porta);
        }
        redisProperties.setSsl("rediss".equalsIgnoreCase(uri.getScheme()));
        if (credenciais != null) {
            aplicarCredenciais(redisProperties, credenciais);
        }

        LOGGER.info(
                "URL do Redis convertida para host {} e porta {}",
                redisProperties.getHost(),
                redisProperties.getPort());
        return true;
    }

    // Mesma semantica do parseUrl do Spring Boot: userinfo sem ':' e senha.
    private static void aplicarCredenciais(RedisProperties redisProperties, String credenciais) {
        int separador = credenciais.indexOf(':');
        if (separador < 0) {
            if (StringUtils.hasText(credenciais)) {
                redisProperties.setPassword(credenciais);
            }
            return;
        }

        String usuario = credenciais.substring(0, separador);
        String senha = credenciais.substring(separador + 1);
        if (StringUtils.hasText(usuario)) {
            redisProperties.setUsername(usuario);
        }
        if (StringUtils.hasText(senha)) {
            redisProperties.setPassword(senha);
        }
    }
}
