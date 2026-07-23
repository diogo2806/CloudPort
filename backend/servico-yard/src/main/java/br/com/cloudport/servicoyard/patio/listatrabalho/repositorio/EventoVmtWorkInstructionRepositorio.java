package br.com.cloudport.servicoyard.patio.listatrabalho.repositorio;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.EventoVmtWorkInstruction;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoVmtWorkInstructionRepositorio extends JpaRepository<EventoVmtWorkInstruction, Long> {

    Optional<EventoVmtWorkInstruction> findByEventId(String eventId);

    Optional<EventoVmtWorkInstruction>
    findFirstByOrdemTrabalhoPatioIdOrderByOcorridoEmDescProcessadoEmDesc(Long ordemTrabalhoPatioId);

    List<EventoVmtWorkInstruction>
    findByOrdemTrabalhoPatioIdOrderByOcorridoEmAscProcessadoEmAsc(Long ordemTrabalhoPatioId);

    long countByTipoEventoInAndOcorridoEmGreaterThanEqualAndOcorridoEmLessThan(
            Collection<TipoEventoVmt> tipos,
            LocalDateTime inicio,
            LocalDateTime fim);
}
