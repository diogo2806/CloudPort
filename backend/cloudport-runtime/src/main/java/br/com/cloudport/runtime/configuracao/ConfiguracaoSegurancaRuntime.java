package br.com.cloudport.runtime.configuracao;

import br.com.cloudport.serviconavio.configuracao.InternalServiceAuthenticationFilter;
import br.com.cloudport.serviconaviosiderurgico.configuracao.CredenciaisSegurancaValidator;
import br.com.cloudport.serviconaviosiderurgico.configuracao.PublicApiClientAuthenticationFilter;
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
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ConfiguracaoSegurancaRuntime {

    private final String jwtSecret;
    private final String allowedOrigins;
    private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;
    private final PublicApiClientAuthenticationFilter publicApiClientAuthenticationFilter;

    public ConfiguracaoSegurancaRuntime(
            @Value("${cloudport.security.jwt.secret}") String jwtSecret,
            @Value("${cloudport.security.cors.allowed-origins:http://localhost:4200,http://localhost:8080}") String allowedOrigins,
            InternalServiceAuthenticationFilter internalServiceAuthenticationFilter,
            PublicApiClientAuthenticationFilter publicApiClientAuthenticationFilter) {
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
        this.internalServiceAuthenticationFilter = internalServiceAuthenticationFilter;
        this.publicApiClientAuthenticationFilter = publicApiClientAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource()).and()
                .csrf(csrf -> csrf
                        .ignoringAntMatchers(
                                "/auth",
                                "/auth/**",
                                "/api/public/v1/**",
                                "/actuator/**")
                        .ignoringRequestMatchers(this::usaAutenticacaoSemCookie))
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests(authorize -> authorize
                        .antMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/auth", "/auth/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/public/line-up-navios", "/line-up", "/line-up/**").permitAll()
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .antMatchers("/api/public/v1/**").hasRole("INTEGRACAO_EXTERNA")
                        .antMatchers("/ws/patio", "/ws/patio/**")
                        .hasAnyRole("ADMIN_PORTO", "PLANEJADOR", "OPERADOR_PATIO", "OPERADOR_GATE",
                                "SERVICE_NAVIO", "SERVICE_SIDERURGICO")
                        .antMatchers("/ws/recursos", "/ws/recursos/**")
                        .hasAnyRole("ADMIN_PORTO", "PLANEJADOR", "OPERADOR_PATIO")
                        .antMatchers("/ws/edi", "/ws/edi/**")
                        .hasAnyRole("ADMIN_PORTO", "PLANEJADOR", "SERVICE_NAVIO", "SERVICE_SIDERURGICO")
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
                .addFilterBefore(publicApiClientAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                .addFilterBefore(internalServiceAuthenticationFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private boolean usaAutenticacaoSemCookie(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean usaBearerToken = StringUtils.hasText(authorization)
                && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
        boolean usaChaveServico = StringUtils.hasText(
                request.getHeader(InternalServiceAuthenticationFilter.HEADER_SERVICE_KEY));
        boolean usaCredenciaisApiPublica = StringUtils.hasText(
                request.getHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID))
                && StringUtils.hasText(
                        request.getHeader(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET));
        return usaBearerToken || usaChaveServico || usaCredenciaisApiPublica;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String segredoValidado = CredenciaisSegurancaValidator.validarSegredoJwt(jwtSecret);
        SecretKey secretKey = new SecretKeySpec(
                segredoValidado.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                    .orElseGet(() -> {
                        String role = jwt.getClaimAsString("role");
                        if (!StringUtils.hasText(role)) {
                            role = jwt.getClaimAsString("perfil");
                        }
                        return StringUtils.hasText(role) ? Collections.singletonList(role) : Collections.emptyList();
                    });

            return roles.stream()
                    .filter(StringUtils::hasText)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT))
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
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:4200");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Correlation-Id",
                InternalServiceAuthenticationFilter.HEADER_SERVICE_KEY,
                PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID,
                PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
