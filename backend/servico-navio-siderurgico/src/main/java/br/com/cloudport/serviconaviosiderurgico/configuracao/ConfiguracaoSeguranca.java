package br.com.cloudport.serviconaviosiderurgico.configuracao;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class ConfiguracaoSeguranca {

    private final String jwtSecret;
    private final String allowedOrigins;
    private final PublicApiClientAuthenticationFilter publicApiClientAuthenticationFilter;

    public ConfiguracaoSeguranca(
            @Value("${cloudport.security.jwt.secret}") String jwtSecret,
            @Value("${cloudport.security.cors.allowed-origins}") String allowedOrigins,
            PublicApiClientAuthenticationFilter publicApiClientAuthenticationFilter) {
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
        this.publicApiClientAuthenticationFilter = publicApiClientAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource()).and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests(authorize -> authorize
                        .antMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .antMatchers("/api/public/v1/**").hasRole("INTEGRACAO_EXTERNA")
                        .antMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/*.css",
                                "/*.js",
                                "/*.mjs",
                                "/*.map",
                                "/*.ico",
                                "/*.png",
                                "/*.svg",
                                "/*.webmanifest",
                                "/*.woff",
                                "/*.woff2").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(publicApiClientAuthenticationFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("cloudport.security.jwt.secret deve ter ao menos 32 caracteres");
        }
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                    .orElseGet(() -> {
                        String role = jwt.getClaimAsString("role");
                        return StringUtils.hasText(role)
                                ? Collections.singletonList(role)
                                : Collections.emptyList();
                    });
            return roles.stream()
                    .filter(StringUtils::hasText)
                    .map(role -> role.startsWith("ROLE_")
                            ? role
                            : "ROLE_" + role.toUpperCase(Locale.ROOT))
                    .distinct()
                    .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        configuration.setAllowedOrigins(origins.isEmpty()
                ? Collections.singletonList("http://localhost:4201")
                : origins);
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                CorrelationIdFilter.HEADER,
                "X-Trace-Id",
                "traceparent",
                PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID,
                PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                CorrelationIdFilter.HEADER,
                "X-Trace-Id",
                "traceparent"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
