package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.EventoConsumido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoConsumidoRepository extends JpaRepository<EventoConsumido, String> {
}
