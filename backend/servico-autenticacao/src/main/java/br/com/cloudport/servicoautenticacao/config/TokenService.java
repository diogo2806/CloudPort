package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TokenService {
    private static final String PAPEL_ROOT = "ROLE_ROOT";
    private static final String PAPEL_TRANSPORTADORA = "ROLE_TRANSPORTADORA";

    private final String secret;
    private final Duration tokenExpiration;
    private final Clock clock;

    @Autowired
    public TokenService(
            @Value("${cloudport.security.jwt.secret}") String secret,
            @Value("${cloudport.security.jwt.expiration}") Duration tokenExpiration) {
        this(secret, tokenExpiration, Clock.systemUTC());
    }

    TokenService(String secret, Duration tokenExpiration, Clock clock) {
        if (tokenExpiration == null || tokenExpiration.isZero() || tokenExpiration.isNegative()) {
            throw new IllegalArgumentException("A duração do token JWT deve ser positiva.");
        }
        this.secret = secret;
        this.tokenExpiration = tokenExpiration;
        this.clock = clock;
    }

    public String generateToken(Usuario usuario) {
        try {
            SecretKey algorithm = signingKey();
            Set<String> papeis = Optional.ofNullable(usuario.getPapeis()).orElseGet(Collections::emptySet).stream()
                    .map(UsuarioPapel::getPapel)
                    .map(Papel::getNome)
                    .filter(StringUtils::hasText)
                    .map(nomePapel -> nomePapel.startsWith("ROLE_")
                            ? nomePapel
                            : "ROLE_" + nomePapel.toUpperCase())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!StringUtils.hasText(usuario.getTransportadoraDocumento())) {
                papeis.remove(PAPEL_TRANSPORTADORA);
            }

            String perfil = papeis.contains(PAPEL_ROOT)
                    ? PAPEL_ROOT
                    : papeis.stream().findFirst().orElse(null);
            Map<String, Object> claims = new HashMap<>();
            if (usuario.getId() != null) {
                claims.put("userId", usuario.getId().toString());
            }
            if (StringUtils.hasText(usuario.getNome())) {
                claims.put("nome", usuario.getNome());
            }
            if (StringUtils.hasText(perfil)) {
                claims.put("perfil", perfil);
            }
            if (!CollectionUtils.isEmpty(papeis)) {
                claims.put("roles", papeis);
            }
            if (StringUtils.hasText(usuario.getTransportadoraDocumento())) {
                claims.put("transportadoraDocumento", usuario.getTransportadoraDocumento());
            }
            if (StringUtils.hasText(usuario.getTransportadoraNome())) {
                claims.put("transportadoraNome", usuario.getTransportadoraNome());
            }

            Instant emitidoEm = Instant.now(clock);
            return Jwts.builder()
                    .setIssuer("auth-api")
                    .setSubject(usuario.getLogin())
                    .setIssuedAt(Date.from(emitidoEm))
                    .setExpiration(Date.from(calcularExpiracao(emitidoEm)))
                    .addClaims(claims)
                    .signWith(algorithm, SignatureAlgorithm.HS256)
                    .compact();
        } catch (JwtException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public Optional<String> validateToken(String token) {
        try {
            SecretKey algorithm = signingKey();
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer("auth-api")
                    .setSigningKey(algorithm)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    Instant calcularExpiracao(Instant emitidoEm) {
        return emitidoEm.plus(tokenExpiration);
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Token secret must be configured");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("Token secret must be at least 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
