package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.IdentificacaoCargoLot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentificacaoCargoLotRepositorio extends JpaRepository<IdentificacaoCargoLot, UUID> {

    Optional<IdentificacaoCargoLot> findByCodigoIgnoreCaseAndAtivoTrue(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<IdentificacaoCargoLot> findByLoteIdOrderByRegistradoEmDesc(UUID loteId);
}
