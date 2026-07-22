package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.AmarradoCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmarradoCargaRepositorio extends JpaRepository<AmarradoCarga, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByLotes_Id(UUID loteId);

    @EntityGraph(attributePaths = {"lotes", "lotes.item", "lotes.item.conhecimento"})
    Optional<AmarradoCarga> findDetalhadoById(UUID id);

    @EntityGraph(attributePaths = {"lotes", "lotes.item", "lotes.item.conhecimento"})
    Optional<AmarradoCarga> findByLotes_Id(UUID loteId);

    @EntityGraph(attributePaths = {"lotes", "lotes.item", "lotes.item.conhecimento"})
    List<AmarradoCarga> findByVisitaNavioIdIgnoreCaseOrderByAtualizadoEmDesc(String visitaNavioId);

    @EntityGraph(attributePaths = {"lotes", "lotes.item", "lotes.item.conhecimento"})
    List<AmarradoCarga> findAllByOrderByAtualizadoEmDesc();
}
