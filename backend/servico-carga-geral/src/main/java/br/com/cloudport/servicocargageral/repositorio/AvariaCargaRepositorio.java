package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.AvariaCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AvariaCargaRepositorio extends JpaRepository<AvariaCarga, UUID> {

    Optional<AvariaCarga> findByCommandId(UUID commandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "evidencias")
    Optional<AvariaCarga> findComBloqueioById(UUID id);

    @EntityGraph(attributePaths = "evidencias")
    List<AvariaCarga> findByLoteIdOrderByCriadoEmDesc(UUID loteId);
}
