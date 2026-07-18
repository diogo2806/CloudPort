package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoTransloadRepositorio extends JpaRepository<OperacaoTransload, UUID> {

    @EntityGraph(attributePaths = "itens")
    Optional<OperacaoTransload> findByCommandId(UUID commandId);
}
