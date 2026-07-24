package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.AuditoriaVinculoEmpresaCarga;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaVinculoEmpresaCargaRepositorio
        extends JpaRepository<AuditoriaVinculoEmpresaCarga, UUID> {
}
