package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
}


/*package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
}
*/