package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaTampaPoraoRepositorio extends JpaRepository<TarefaTampaPorao, Long> {
    List<TarefaTampaPorao> findByTampaIdOrderByOrdemOperacionalAscIdAsc(Long tampaId);
    List<TarefaTampaPorao> findByDependenciaId(Long dependenciaId);
    boolean existsByTampaId(Long tampaId);
}
