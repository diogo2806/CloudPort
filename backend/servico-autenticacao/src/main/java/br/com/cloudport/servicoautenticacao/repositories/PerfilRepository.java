package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.domain.user.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByName(String name);
}
