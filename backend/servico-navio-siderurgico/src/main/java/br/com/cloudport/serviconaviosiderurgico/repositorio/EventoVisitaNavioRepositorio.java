package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoVisitaNavio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoVisitaNavioRepositorio extends JpaRepository<EventoVisitaNavio, Long> {
    List<EventoVisitaNavio> findByVisitaNavioIdOrderByCriadoEmDesc(Long visitaNavioId);
}
