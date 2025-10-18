package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.CargaPatio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargaPatioRepositorio extends JpaRepository<CargaPatio, Long> {

    Optional<CargaPatio> findByCodigo(String codigo);
}
