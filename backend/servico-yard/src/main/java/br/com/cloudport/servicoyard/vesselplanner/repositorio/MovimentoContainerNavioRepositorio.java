package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoContainerNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoContainerNavio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoContainerNavioRepositorio extends JpaRepository<MovimentoContainerNavio, Long> {
    boolean existsByEstivagemIdAndSlotIdAndStatus(
            Long estivagemId,
            Long slotId,
            StatusMovimentoContainerNavio status);

    List<MovimentoContainerNavio> findByEstivagemIdOrderByOrdemSequenciaAscIdAsc(Long estivagemId);
}
