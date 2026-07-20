package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.ProgramacaoDocaCarga;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ProgramacaoDocaCargaRepositorio extends JpaRepository<ProgramacaoDocaCarga, UUID> {

    List<ProgramacaoDocaCarga> findAllByOrderByJanelaInicioDesc();

    List<ProgramacaoDocaCarga> findByJanelaInicioLessThanAndJanelaFimGreaterThanOrderByJanelaInicioAsc(
            OffsetDateTime fim,
            OffsetDateTime inicio);

    Optional<ProgramacaoDocaCarga> findByOperacao_Id(UUID operacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProgramacaoDocaCarga> findComBloqueioByOperacao_Id(UUID operacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProgramacaoDocaCarga> findByStatusInAndJanelaInicioLessThanAndJanelaFimGreaterThan(
            Collection<StatusProgramacaoDocaCarga> status,
            OffsetDateTime fim,
            OffsetDateTime inicio);

    boolean existsByOperacao_IdAndStatusIn(
            UUID operacaoId,
            Collection<StatusProgramacaoDocaCarga> status);
}
