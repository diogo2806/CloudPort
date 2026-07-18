package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.StatusTarefaTampaPorao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaTampaPoraoRepositorio extends JpaRepository<TarefaTampaPorao, Long> {

    List<TarefaTampaPorao> findByTampaIdOrderByCriadoEmAsc(Long tampaId);

    Optional<TarefaTampaPorao> findFirstByTampaIdAndStatusOrderByCriadoEmDesc(
            Long tampaId,
            StatusTarefaTampaPorao status);

    Optional<TarefaTampaPorao> findFirstByTampaIdAndStatusNotOrderByCriadoEmDesc(
            Long tampaId,
            StatusTarefaTampaPorao status);
}
