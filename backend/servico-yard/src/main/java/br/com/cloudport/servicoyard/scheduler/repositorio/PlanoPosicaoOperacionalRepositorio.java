package br.com.cloudport.servicoyard.scheduler.repositorio;

import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.PlanoPosicaoOperacional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanoPosicaoOperacionalRepositorio extends JpaRepository<PlanoPosicaoOperacional, Long> {

    Optional<PlanoPosicaoOperacional> findByAssinaturaEntradaAndCodigoContainerIgnoreCase(
            String assinaturaEntrada,
            String codigoContainer);

    List<PlanoPosicaoOperacional> findAllByOrderByHorizonteInicioAscCodigoContainerAsc();

    List<PlanoPosicaoOperacional> findByEstadoInAndHorizonteFimAfterOrderByHorizonteInicioAsc(
            Collection<EstadoPlanoPosicaoOperacional> estados,
            LocalDateTime referencia);

    Optional<PlanoPosicaoOperacional> findFirstByCodigoContainerIgnoreCaseAndEstadoInOrderByAtualizadoEmDesc(
            String codigoContainer,
            Collection<EstadoPlanoPosicaoOperacional> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plano from PlanoPosicaoOperacional plano where plano.id = :id")
    Optional<PlanoPosicaoOperacional> findByIdForUpdate(@Param("id") Long id);
}
