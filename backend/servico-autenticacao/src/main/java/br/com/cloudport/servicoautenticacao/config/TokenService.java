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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(Usuario usuario){
        try{
            SecretKey algorithm = signingKey();
            Set<String> papeis = Optional.ofNullable(usuario.getPapeis()).orElseGet(Collections::emptySet).stream()
                    .map(UsuarioPapel::getPapel)
                    .map(Papel::getNome)
                    .filter(StringUtils::hasText)
                    .map(nomePapel -> nomePapel.startsWith("ROLE_") ? nomePapel : "ROLE_" + nomePapel.toUpperCase())
                    .collect(Collectors.toSet());

            String perfil = papeis.stream().findFirst().orElse(null);

            var claims = new HashMap<String, Object>();
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

            return Jwts.builder()
                    .setIssuer("auth-api")
                    .setSubject(usuario.getLogin())
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
