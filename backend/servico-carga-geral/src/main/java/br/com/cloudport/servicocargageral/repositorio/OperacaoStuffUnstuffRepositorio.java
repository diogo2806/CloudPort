package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OperacaoStuffUnstuffRepositorio extends JpaRepository<OperacaoStuffUnstuff, UUID> {

    @EntityGraph(attributePaths = {"itens", "itens.lote"})
    List<OperacaoStuffUnstuff> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = {"itens", "itens.lote"})
    Optional<OperacaoStuffUnstuff> findDetalhadaById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"itens", "itens.lote"})
    Optional<OperacaoStuffUnstuff> findComBloqueioById(UUID id);
}
