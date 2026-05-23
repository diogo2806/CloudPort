package br.com.cloudport.servicoautenticacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.model.Usuario;

import java.util.HashSet;
import java.util.Optional;

@SpringBootTest
class ServicoAutenticacaoApplicationTests {

    @Autowired
    private TokenService tokenService;

    @Test
    void contextLoads() {
    }

    @Test
    void generateAndValidateToken() {
        Usuario usuario = new Usuario("test", "pass", new HashSet<>());
        String token = tokenService.generateToken(usuario);
        assertNotNull(token);
        assertEquals(Optional.of(usuario.getLogin()), tokenService.validateToken(token));
    }

    @Test
    void invalidTokenReturnsEmpty() {
        String invalidToken = "invalid";
        assertTrue(tokenService.validateToken(invalidToken).isEmpty());
    }

}
