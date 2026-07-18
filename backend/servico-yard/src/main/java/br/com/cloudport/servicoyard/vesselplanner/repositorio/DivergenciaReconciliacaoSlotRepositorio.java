package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.DivergenciaReconciliacaoSlot;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SeveridadeDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusDivergenciaReconciliacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivergenciaReconciliacaoSlotRepositorio
        extends JpaRepository<DivergenciaReconciliacaoSlot, Long> {

    List<DivergenciaReconciliacaoSlot> findByEstivagemIdOrderByCriadoEmAsc(Long estivagemId);

    Optional<DivergenciaReconciliacaoSlot> findByIdAndEstivagemId(Long id, Long estivagemId);

    long countByEstivagemIdAndSeveridadeAndStatus(
            Long estivagemId,
            SeveridadeDivergenciaReconciliacao severidade,
            StatusDivergenciaReconciliacao status);
}
