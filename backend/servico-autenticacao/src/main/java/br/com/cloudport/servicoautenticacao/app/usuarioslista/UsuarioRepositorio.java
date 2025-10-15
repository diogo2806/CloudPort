package br.com.cloudport.servicoautenticacao.app.usuarioslista;

import br.com.cloudport.servicoautenticacao.model.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByLogin(String login);
}
