package br.com.cloudport.servicogate.app.configuracoes;

import br.com.cloudport.servicogate.model.Transportadora;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportadoraRepository extends JpaRepository<Transportadora, Long> {

    Optional<Transportadora> findByDocumento(String documento);
}
