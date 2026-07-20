package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ConsumoOrdemLiberacaoStuffUnstuff;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumoOrdemLiberacaoStuffUnstuffRepositorio
        extends JpaRepository<ConsumoOrdemLiberacaoStuffUnstuff, UUID> {
    boolean existsByOperacaoIdAndCommandId(UUID operacaoId, UUID commandId);
}
