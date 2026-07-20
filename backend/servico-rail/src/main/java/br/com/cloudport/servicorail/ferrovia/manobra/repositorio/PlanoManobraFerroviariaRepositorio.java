package br.com.cloudport.servicorail.ferrovia.manobra.repositorio;

import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria.StatusPlanoManobra;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoManobraFerroviariaRepositorio extends JpaRepository<PlanoManobraFerroviaria, Long> {

    List<PlanoManobraFerroviaria> findByVisitaTremIdOrderBySequenciaAsc(Long idVisita);

    List<PlanoManobraFerroviaria> findByLinhaIgnoreCaseAndStatusInOrderByInicioPrevistoAsc(
            String linha,
            Collection<StatusPlanoManobra> status);

    Optional<PlanoManobraFerroviaria> findByIdAndVisitaTremId(Long id, Long idVisita);

    boolean existsByVisitaTremIdAndSequencia(Long idVisita, Integer sequencia);
}
