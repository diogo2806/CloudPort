package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.PlanoOperacionalCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PlanoOperacionalCargaRepositorio extends JpaRepository<PlanoOperacionalCarga, UUID> {

    boolean existsByNumeroIgnoreCase(String numero);

    @EntityGraph(attributePaths = {"itens", "itens.lote", "itens.lote.item", "itens.lote.item.conhecimento"})
    Optional<PlanoOperacionalCarga> findDetalhadoById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"itens", "itens.lote", "itens.lote.movimentacoes"})
    Optional<PlanoOperacionalCarga> findComBloqueioById(UUID id);

    @EntityGraph(attributePaths = {"itens", "itens.lote"})
    List<PlanoOperacionalCarga> findAllByOrderByCriadoEmDesc();
}
