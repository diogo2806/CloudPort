package br.com.cloudport.servicoautenticacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.model.User;

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
        User user = new User("test", "pass", new HashSet<>());
        String token = tokenService.generateToken(user);
        assertNotNull(token);
        assertEquals(Optional.of(user.getLogin()), tokenService.validateToken(token));
    }

    @Test
    void invalidTokenReturnsEmpty() {
        String invalidToken = "invalid";
        assertTrue(tokenService.validateToken(invalidToken).isEmpty());
    }

}
