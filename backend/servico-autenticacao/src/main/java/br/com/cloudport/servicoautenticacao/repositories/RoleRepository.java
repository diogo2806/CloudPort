package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    List<Role> findAllByOrderByIdAsc();

}
