package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ConhecimentoCarga;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConhecimentoCargaRepositorio extends JpaRepository<ConhecimentoCarga, UUID> {

    boolean existsByNumeroIgnoreCase(String numero);

    @EntityGraph(attributePaths = {"itens", "itens.lotes", "itens.lotes.movimentacoes"})
    Optional<ConhecimentoCarga> findDetalhadoById(UUID id);
}
