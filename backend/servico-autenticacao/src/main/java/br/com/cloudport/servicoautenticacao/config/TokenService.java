package br.com.cloudport.servicoautenticacao.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user){
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);
            Set<String> roles = Optional.ofNullable(user.getRoles()).orElseGet(java.util.Collections::emptySet).stream()
                    .map(UserRole::getRole)
                    .map(Role::getName)
                    .filter(StringUtils::hasText)
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                    .collect(Collectors.toSet());

            String perfil = roles.stream().findFirst().orElse(null);

            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getLogin())
                    .withClaim("userId", user.getId() != null ? user.getId().toString() : null)
                    .withClaim("nome", user.getNome())
                    .withClaim("perfil", perfil)
                    .withClaim("roles", CollectionUtils.isEmpty(roles) ? null : roles)
                    .withClaim("transportadoraDocumento", user.getTransportadoraDocumento())
                    .withClaim("transportadoraNome", user.getTransportadoraNome())
                    .withExpiresAt(Date.from(genExpirationDate()))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public Optional<String> validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var subject = JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (JWTVerificationException exception){
            // SECURITY: evita autenticação com tokens inválidos propagando ausência de subject
            return Optional.empty();
        }
    }

    private Instant genExpirationDate(){
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
