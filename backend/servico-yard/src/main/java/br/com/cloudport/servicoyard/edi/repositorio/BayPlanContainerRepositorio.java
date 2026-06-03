package br.com.cloudport.servicoyard.edi.repositorio;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BayPlanContainerRepositorio extends JpaRepository<BayPlanContainer, Long> {

    List<BayPlanContainer> findByBayPlanId(Long bayPlanId);

    Optional<BayPlanContainer> findByBayPlanIdAndCodigoContainer(Long bayPlanId, String codigoContainer);

    List<BayPlanContainer> findByBayPlanIdAndTipoOperacao(Long bayPlanId, TipoOperacaoBayPlan tipo);
}
