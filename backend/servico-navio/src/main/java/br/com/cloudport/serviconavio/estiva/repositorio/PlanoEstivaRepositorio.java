package br.com.cloudport.serviconavio.estiva.repositorio;

import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoEstivaRepositorio extends JpaRepository<PlanoEstiva, Long> {

    Optional<PlanoEstiva> findByEscalaId(Long escalaId);

    boolean existsByEscalaId(Long escalaId);
}
