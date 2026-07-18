package br.com.cloudport.servicoyard.vesselplanner.repositorio;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TampaPoraoRepositorio extends JpaRepository<TampaPorao, Long> {

    List<TampaPorao> findByEstivagemIdOrderByCodigoAsc(Long estivagemId);

    Optional<TampaPorao> findByEstivagemIdAndCodigo(Long estivagemId, String codigo);
}
