package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoGuindasteVisita;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoGuindasteVisitaRepositorio extends JpaRepository<PlanoGuindasteVisita, Long> {
    List<PlanoGuindasteVisita> findByVisitaNavioIdOrderBySequenciaAsc(Long visitaNavioId);
    void deleteByVisitaNavioId(Long visitaNavioId);
}
