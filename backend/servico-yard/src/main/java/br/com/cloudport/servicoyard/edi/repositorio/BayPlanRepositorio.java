package br.com.cloudport.servicoyard.edi.repositorio;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BayPlanRepositorio extends JpaRepository<BayPlan, Long> {

    Optional<BayPlan> findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
            String codigoNavio, String codigoViagem);

    List<BayPlan> findByCodigoNavioOrderByAtualizadoEmDesc(String codigoNavio);

    List<BayPlan> findByStatusIn(List<StatusBayPlan> statuses);
}
