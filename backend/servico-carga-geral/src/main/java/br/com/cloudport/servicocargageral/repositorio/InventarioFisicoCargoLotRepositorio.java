package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.InventarioFisicoCargoLot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InventarioFisicoCargoLotRepositorio extends JpaRepository<InventarioFisicoCargoLot, UUID> {

    Optional<InventarioFisicoCargoLot> findByCommandIdAbertura(UUID commandIdAbertura);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "contagens")
    Optional<InventarioFisicoCargoLot> findComBloqueioById(UUID id);

    @EntityGraph(attributePaths = "contagens")
    List<InventarioFisicoCargoLot> findAllByOrderByAbertoEmDesc();
}
