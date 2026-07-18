package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OperacaoTransloadRepositorio extends JpaRepository<OperacaoTransload, UUID> {

    @EntityGraph(attributePaths = "itens")
    Optional<OperacaoTransload> findByCommandId(UUID commandId);

    @EntityGraph(attributePaths = "itens")
    Optional<OperacaoTransload> findDetalhadoById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "itens")
    Optional<OperacaoTransload> findComBloqueioById(UUID id);
}
