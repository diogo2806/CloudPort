package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OrdemTrabalhoCarga;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemTrabalhoCargaRepositorio extends JpaRepository<OrdemTrabalhoCarga, UUID> {
    boolean existsByNumeroIgnoreCase(String numero);
}
