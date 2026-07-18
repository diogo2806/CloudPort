package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.EventoInstrucaoVmt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoInstrucaoVmtRepositorio extends JpaRepository<EventoInstrucaoVmt, Long> {

    boolean existsByEventId(String eventId);

    List<EventoInstrucaoVmt> findByInstrucaoIdOrderByProcessadoEmAsc(Long instrucaoId);
}
