package br.com.cloudport.servicoyard.scheduler.repositorio;

import br.com.cloudport.servicoyard.scheduler.modelo.HistoricoPlanoPosicaoOperacional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoPlanoPosicaoOperacionalRepositorio
        extends JpaRepository<HistoricoPlanoPosicaoOperacional, Long> {

    List<HistoricoPlanoPosicaoOperacional> findByPlanoIdOrderByOcorridoEmDesc(Long planoId);
}
