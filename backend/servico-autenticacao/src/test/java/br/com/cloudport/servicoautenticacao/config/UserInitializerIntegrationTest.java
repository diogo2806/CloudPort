package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class UserInitializerIntegrationTest {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    void usuarioAdministradorInicialPossuiPapeisPersistidos() {
        Usuario administrador = usuarioRepositorio.findByLogin("admin@cloudport.test").orElse(null);
        assertNotNull(administrador, "Usuário administrador inicial não foi criado");

        assertNotNull(administrador.getPapeis(), "Coleção de papéis não deve ser nula");
        assertFalse(administrador.getPapeis().isEmpty(), "Usuário administrador deve possuir ao menos um papel associado");
        administrador.getPapeis().forEach(usuarioPapel ->
                assertNotNull(usuarioPapel.getPapel(), "Vínculo deve manter referência ao papel"));
    }
}
