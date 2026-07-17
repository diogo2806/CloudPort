package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.PerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusPerfilGeometriaNavio;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilGeometriaNavioRepositorio extends JpaRepository<PerfilGeometriaNavio, Long> {

    @EntityGraph(attributePaths = "slots")
    Optional<PerfilGeometriaNavio> findFirstByCodigoNavioIgnoreCaseAndStatusOrderByVersaoPerfilDesc(
            String codigoNavio,
            StatusPerfilGeometriaNavio status);
}
