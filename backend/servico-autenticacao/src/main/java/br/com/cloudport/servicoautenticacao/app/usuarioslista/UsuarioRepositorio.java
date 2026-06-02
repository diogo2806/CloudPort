package br.com.cloudport.servicoautenticacao.app.usuarioslista;

import br.com.cloudport.servicoautenticacao.model.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {
    @Query("select distinct u from users u left join fetch u.papeis up left join fetch up.papel left join fetch up.usuario where u.login = :login")
    Optional<Usuario> findByLogin(@Param("login") String login);
}
