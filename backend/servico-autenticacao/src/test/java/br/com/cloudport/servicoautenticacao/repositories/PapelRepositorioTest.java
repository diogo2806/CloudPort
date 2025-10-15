package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.StatusUsuarioEnum;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PapelRepositorioTest {

    @Autowired
    private PapelRepositorio papelRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private UsuarioPapelRepositorio usuarioPapelRepositorio;

    @Test
    void buscarPorNomeRetornaPapelComVinculosDeUsuarios() {
        Papel papel = papelRepositorio.save(new Papel("ADMIN"));

        Usuario usuario = new Usuario(UUID.randomUUID(), "john.doe", "senha", new HashSet<>());
        usuarioRepositorio.save(usuario);

        UsuarioPapel vinculo = new UsuarioPapel(usuario, papel);
        vinculo.setStatus(StatusUsuarioEnum.ATIVO);
        usuario.getPapeis().add(vinculo);
        papel.getUsuarioPapeis().add(vinculo);
        usuarioPapelRepositorio.save(vinculo);

        Papel persistido = papelRepositorio.findByNome("ADMIN").orElseThrow();

        assertThat(persistido.getNome()).isEqualTo("ADMIN");
        assertThat(persistido.getUsuarioPapeis())
                .hasSize(1)
                .allSatisfy(usuarioPapel -> {
                    assertThat(usuarioPapel.getUsuario().getId()).isEqualTo(usuario.getId());
                    assertThat(usuarioPapel.getPapel().getId()).isEqualTo(papel.getId());
                    assertThat(usuarioPapel.getStatus()).isEqualTo(StatusUsuarioEnum.ATIVO);
                });
    }
}
