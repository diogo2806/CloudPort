package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoOutboxSequenciaGuindaste;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoOutboxSequenciaGuindasteRepositorio
        extends JpaRepository<EventoOutboxSequenciaGuindaste, Long> {

    boolean existsByEventKey(String eventKey);
}
