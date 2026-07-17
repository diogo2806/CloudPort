package br.com.cloudport.servicoyard.estivagembulk.repositorio;

import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.StatusPlanoEstiva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoEstivaBulkRepositorio extends JpaRepository<PlanoEstivaBulk, Long> {
    List<PlanoEstivaBulk> findByNavioIdOrderByCriadoEmDesc(Long navioId);
    List<PlanoEstivaBulk> findByNavioIdAndVisitaNavioIdOrderByCriadoEmDesc(Long navioId, Long visitaNavioId);
    List<PlanoEstivaBulk> findByStatus(StatusPlanoEstiva status);
}
