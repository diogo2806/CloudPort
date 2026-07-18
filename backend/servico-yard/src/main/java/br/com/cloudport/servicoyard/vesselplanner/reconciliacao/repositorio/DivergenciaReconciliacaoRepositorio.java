package br.com.cloudport.servicoyard.vesselplanner.reconciliacao.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.reconciliacao.modelo.DivergenciaReconciliacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivergenciaReconciliacaoRepositorio
        extends JpaRepository<DivergenciaReconciliacao, Long> {

    List<DivergenciaReconciliacao> findByReconciliacaoIdOrderBySeveridadeDescDetectadaEmAsc(
            Long reconciliacaoId);

    Optional<DivergenciaReconciliacao> findByIdAndReconciliacaoId(Long id, Long reconciliacaoId);
}
