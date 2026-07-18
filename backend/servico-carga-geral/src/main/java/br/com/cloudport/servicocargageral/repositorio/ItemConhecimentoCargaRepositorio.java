package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemConhecimentoCargaRepositorio extends JpaRepository<ItemConhecimentoCarga, UUID> {

    @EntityGraph(attributePaths = {"conhecimento", "lotes"})
    Optional<ItemConhecimentoCarga> findDetalhadoById(UUID id);
}
