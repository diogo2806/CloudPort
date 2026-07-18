package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.DependenciaTarefaTampaPorao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaTarefaTampaPoraoRepositorio
        extends JpaRepository<DependenciaTarefaTampaPorao, Long> {

    List<DependenciaTarefaTampaPorao> findByTarefaId(Long tarefaId);
}
