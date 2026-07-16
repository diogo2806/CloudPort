package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoInternoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoInternoProcessadoRepositorio extends JpaRepository<EventoInternoProcessado, String> {
}
