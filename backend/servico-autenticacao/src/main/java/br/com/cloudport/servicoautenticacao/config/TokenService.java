package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user){
        try{
            SecretKey algorithm = signingKey();
            Set<String> roles = Optional.ofNullable(user.getRoles()).orElseGet(Collections::emptySet).stream()
                    .map(UserRole::getRole)
                    .map(Role::getName)
                    .filter(StringUtils::hasText)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                    .collect(Collectors.toSet());

            String perfil = roles.stream().findFirst().orElse(null);

            var claims = new HashMap<String, Object>();
            if (user.getId() != null) {
                claims.put("userId", user.getId().toString());
            }
            if (StringUtils.hasText(user.getNome())) {
                claims.put("nome", user.getNome());
            }
            if (StringUtils.hasText(perfil)) {
                claims.put("perfil", perfil);
            }
            if (!CollectionUtils.isEmpty(roles)) {
                claims.put("roles", roles);
            }
            if (StringUtils.hasText(user.getTransportadoraDocumento())) {
                claims.put("transportadoraDocumento", user.getTransportadoraDocumento());
            }
            if (StringUtils.hasText(user.getTransportadoraNome())) {
                claims.put("transportadoraNome", user.getTransportadoraNome());
            }

            return Jwts.builder()
                    .setIssuer("auth-api")
                    .setSubject(user.getLogin())
                    .setExpiration(Date.from(genExpirationDate()))
                    .addClaims(claims)
                    .signWith(algorithm, SignatureAlgorithm.HS256)
                    .compact();
        } catch (JwtException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public Optional<String> validateToken(String token){
        try {
            SecretKey algorithm = signingKey();
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer("auth-api")
                    .setSigningKey(algorithm)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception){
            // SECURITY: evita autenticação com tokens inválidos propagando ausência de subject
            return Optional.empty();
        }
    }

    private Instant genExpirationDate(){
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
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
