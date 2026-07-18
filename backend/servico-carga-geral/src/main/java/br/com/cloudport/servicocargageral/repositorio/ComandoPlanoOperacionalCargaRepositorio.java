package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.ComandoPlanoOperacionalCarga;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComandoPlanoOperacionalCargaRepositorio extends JpaRepository<ComandoPlanoOperacionalCarga, UUID> {
    Optional<ComandoPlanoOperacionalCarga> findByPlanoIdAndCommandIdIgnoreCase(UUID planoId, String commandId);
}
