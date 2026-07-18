package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.AlocacaoCargoLot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AlocacaoCargoLotRepositorio extends JpaRepository<AlocacaoCargoLot, UUID> {

    Optional<AlocacaoCargoLot> findByCommandId(UUID commandId);

    Optional<AlocacaoCargoLot> findByReservaCapacidadeId(UUID reservaCapacidadeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AlocacaoCargoLot> findComBloqueioById(UUID id);

    List<AlocacaoCargoLot> findByLoteIdOrderByCriadoEmDesc(UUID loteId);
}
