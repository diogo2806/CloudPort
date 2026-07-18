package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ComandoExecucaoStuffUnstuff;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComandoExecucaoStuffUnstuffRepositorio
        extends JpaRepository<ComandoExecucaoStuffUnstuff, UUID> {

    Optional<ComandoExecucaoStuffUnstuff> findByOperacaoIdAndCommandId(UUID operacaoId, UUID commandId);
}
