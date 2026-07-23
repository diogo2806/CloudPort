package br.com.cloudport.servicoyard.patio.purgatorio.repositorio;

import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CasoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.EstadoPurgatorioWorkInstruction;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasoPurgatorioWorkInstructionRepositorio
        extends JpaRepository<CasoPurgatorioWorkInstruction, Long> {

    Optional<CasoPurgatorioWorkInstruction> findByChaveIdempotencia(String chaveIdempotencia);

    boolean existsByWorkQueueIdAndEstadoIn(Long workQueueId,
                                            Collection<EstadoPurgatorioWorkInstruction> estados);

    List<CasoPurgatorioWorkInstruction> findByEstadoInOrderByCriadoEmAsc(
            Collection<EstadoPurgatorioWorkInstruction> estados);

    List<CasoPurgatorioWorkInstruction> findByWorkQueueIdOrderByCriadoEmAsc(Long workQueueId);
}
