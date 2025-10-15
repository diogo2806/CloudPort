package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import br.com.cloudport.servicoautenticacao.repositories.UsuarioPapelRepositorio;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserInitializerIntegrationTest {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private UsuarioPapelRepositorio usuarioPapelRepositorio;

    @Test
    void usuarioAdministradorInicialPossuiPapeisPersistidos() {
        Usuario administrador = usuarioRepositorio.findByLogin("gitpod").orElse(null);
        assertNotNull(administrador, "Usuário administrador inicial não foi criado");

        assertNotNull(administrador.getPapeis(), "Coleção de papéis não deve ser nula");
        assertFalse(administrador.getPapeis().isEmpty(), "Usuário administrador deve possuir ao menos um papel associado");
        administrador.getPapeis().forEach(usuarioPapel -> {
            assertNotNull(usuarioPapel.getUsuario(), "Vínculo deve manter referência ao usuário");
            assertEquals(administrador.getId(), usuarioPapel.getUsuario().getId(), "Vínculo deve estar ligado ao administrador");
        });

        List<UsuarioPapel> papeisPersistidos = usuarioPapelRepositorio.findAll();
        assertFalse(papeisPersistidos.isEmpty(), "Deve haver registros de vínculo usuário-papel persistidos");
        papeisPersistidos.forEach(usuarioPapel -> {
            assertNotNull(usuarioPapel.getId(), "Vínculo deve possuir identificador persistido");
            assertNotNull(usuarioPapel.getUsuario(), "Vínculo persistido deve estar vinculado a um usuário");
        });
    }
}
