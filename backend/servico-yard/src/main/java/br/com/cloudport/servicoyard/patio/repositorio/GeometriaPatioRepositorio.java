package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.GeometriaPatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeometriaPatioRepositorio extends JpaRepository<GeometriaPatio, Long> {

    List<GeometriaPatio> findAllByAtivaTrueOrderByTipoAscCodigoAsc();

    Optional<GeometriaPatio> findByCodigoIgnoreCase(String codigo);
}
