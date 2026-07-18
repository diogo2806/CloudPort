package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoSequenciaGuindasteRepositorio extends JpaRepository<ExecucaoSequenciaGuindaste, Long> {

    @EntityGraph(attributePaths = "movimentos")
    Optional<ExecucaoSequenciaGuindaste> findByEstivagemId(Long estivagemId);

    @Override
    @EntityGraph(attributePaths = "movimentos")
    Optional<ExecucaoSequenciaGuindaste> findById(Long id);
}
