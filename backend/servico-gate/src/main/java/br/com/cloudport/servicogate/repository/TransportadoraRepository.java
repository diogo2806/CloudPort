package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.model.Transportadora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportadoraRepository extends JpaRepository<Transportadora, Long> {

    Optional<Transportadora> findByDocumento(String documento);
}
