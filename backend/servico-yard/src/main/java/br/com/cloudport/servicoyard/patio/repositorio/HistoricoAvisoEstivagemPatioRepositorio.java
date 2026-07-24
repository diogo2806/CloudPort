package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.HistoricoAvisoEstivagemPatio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoAvisoEstivagemPatioRepositorio
        extends JpaRepository<HistoricoAvisoEstivagemPatio, Long> {

    List<HistoricoAvisoEstivagemPatio> findByAvisoIdOrderByOcorridoEmAsc(Long avisoId);
}
