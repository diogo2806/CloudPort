package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OrdemLiberacaoStuffUnstuff;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemLiberacaoStuffUnstuffRepositorio extends JpaRepository<OrdemLiberacaoStuffUnstuff, UUID> {

    Optional<OrdemLiberacaoStuffUnstuff> findByOperacaoId(UUID operacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemLiberacaoStuffUnstuff o where o.operacaoId = :operacaoId")
    Optional<OrdemLiberacaoStuffUnstuff> findComBloqueioByOperacaoId(@Param("operacaoId") UUID operacaoId);
}
