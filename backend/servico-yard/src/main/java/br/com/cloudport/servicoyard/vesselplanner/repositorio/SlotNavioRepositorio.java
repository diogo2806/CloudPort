package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotNavioRepositorio extends JpaRepository<SlotNavio, Long> {
    List<SlotNavio> findByEstivagemId(Long estivagem_id);
    Optional<SlotNavio> findByEstivagemIdAndBayAndRowBayAndTier(Long estivagemId, int bay, int row, int tier);
    List<SlotNavio> findByEstivagemIdAndCodigoContainerNotNull(Long estivagemId);
}
