package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;


public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    long countByRoleId(Long roleId);
    boolean existsByRoleId(Long id);
}
