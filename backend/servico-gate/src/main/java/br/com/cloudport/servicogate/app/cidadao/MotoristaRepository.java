package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.model.Motorista;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    Optional<Motorista> findByDocumento(String documento);

    List<Motorista> findByTransportadoraIdOrderByNomeAsc(Long transportadoraId);
}
