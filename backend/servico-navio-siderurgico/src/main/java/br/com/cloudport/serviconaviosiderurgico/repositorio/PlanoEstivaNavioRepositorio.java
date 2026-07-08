package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoEstivaNavio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoEstivaNavioRepositorio extends JpaRepository<PlanoEstivaNavio, Long> {
    Optional<PlanoEstivaNavio> findFirstByVisitaNavioIdOrderByVersaoDesc(Long visitaNavioId);
    List<PlanoEstivaNavio> findByVisitaNavioIdOrderByVersaoDesc(Long visitaNavioId);
}
