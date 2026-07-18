package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.InventarioFisicoCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InventarioFisicoCargaRepositorio extends JpaRepository<InventarioFisicoCarga, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = {"contagens", "contagens.lote"})
    Optional<InventarioFisicoCarga> findDetalhadoById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"contagens", "contagens.lote"})
    Optional<InventarioFisicoCarga> findComBloqueioById(UUID id);

    @EntityGraph(attributePaths = {"contagens", "contagens.lote"})
    List<InventarioFisicoCarga> findAllByOrderByCriadoEmDesc();
}
