package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusEstivagemPlan;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EstivagemPlanRepositorio extends JpaRepository<EstivagemPlan, Long> {
    List<EstivagemPlan> findByCodigoNavioOrderByCriadoEmDesc(String codigoNavio);
    Optional<EstivagemPlan> findTopByCodigoNavioAndCodigoViagemOrderByCriadoEmDesc(String codigoNavio, String codigoViagem);
    List<EstivagemPlan> findByStatus(StatusEstivagemPlan status);
    Optional<EstivagemPlan> findByBayPlanId(Long bayPlanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EstivagemPlan> findLockedById(Long id);
}
