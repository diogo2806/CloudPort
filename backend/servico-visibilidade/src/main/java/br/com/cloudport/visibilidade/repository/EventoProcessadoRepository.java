package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.EventoProcessado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, Long> {

    Optional<EventoProcessado> findByIdentidadeEvento(String identidadeEvento);
}
