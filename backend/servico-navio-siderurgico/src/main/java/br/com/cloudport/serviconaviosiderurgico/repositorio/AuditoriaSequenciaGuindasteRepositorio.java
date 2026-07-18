package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.AuditoriaSequenciaGuindaste;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaSequenciaGuindasteRepositorio extends JpaRepository<AuditoriaSequenciaGuindaste, Long> {

    List<AuditoriaSequenciaGuindaste> findByMovementIdOrderByOccurredAtDesc(String movementId);

    boolean existsByMovementIdAndTypeAndReason(String movementId, String type, String reason);
}
