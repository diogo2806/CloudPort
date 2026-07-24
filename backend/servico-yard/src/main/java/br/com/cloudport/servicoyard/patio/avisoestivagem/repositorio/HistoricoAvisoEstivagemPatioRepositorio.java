package br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.HistoricoAvisoEstivagemPatio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoAvisoEstivagemPatioRepositorio
        extends JpaRepository<HistoricoAvisoEstivagemPatio, Long> {

    List<HistoricoAvisoEstivagemPatio> findByAvisoIdOrderByCriadoEmAscIdAsc(Long avisoId);
}
