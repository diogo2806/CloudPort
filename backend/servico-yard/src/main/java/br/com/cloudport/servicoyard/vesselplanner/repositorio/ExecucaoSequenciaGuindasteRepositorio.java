package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ExecucaoSequenciaGuindasteRepositorio extends JpaRepository<ExecucaoSequenciaGuindaste, Long> {

    @EntityGraph(attributePaths = "movimentos")
    Optional<ExecucaoSequenciaGuindaste> findByEstivagemId(Long estivagemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "movimentos")
    Optional<ExecucaoSequenciaGuindaste> findLockedByEstivagemId(Long estivagemId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "movimentos")
    Optional<ExecucaoSequenciaGuindaste> findById(Long id);
}
