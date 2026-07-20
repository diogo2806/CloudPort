package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.EventoOperacionalGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoEventoOperacionalGuindaste;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EventoOperacionalGuindasteRepositorio
        extends JpaRepository<EventoOperacionalGuindaste, Long> {

    List<EventoOperacionalGuindaste> findByExecucaoIdOrderByInicioDesc(Long execucaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EventoOperacionalGuindaste> findByExecucaoIdAndGuindasteIdAndTipoOrderByInicioAsc(
            Long execucaoId,
            Integer guindasteId,
            TipoEventoOperacionalGuindaste tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EventoOperacionalGuindaste> findLockedById(Long id);
}
