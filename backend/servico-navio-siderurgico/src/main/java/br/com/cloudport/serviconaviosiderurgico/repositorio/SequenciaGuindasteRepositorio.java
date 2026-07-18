package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.SequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SequenciaGuindasteRepositorio
        extends JpaRepository<SequenciaGuindaste, Long>, JpaSpecificationExecutor<SequenciaGuindaste> {

    Optional<SequenciaGuindaste> findByMovementId(String movementId);

    List<SequenciaGuindaste> findByStatusIn(List<StatusSequenciaGuindaste> status);
}
