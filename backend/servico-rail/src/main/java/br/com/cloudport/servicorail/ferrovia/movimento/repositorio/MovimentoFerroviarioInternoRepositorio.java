package br.com.cloudport.servicorail.ferrovia.movimento.repositorio;

import br.com.cloudport.servicorail.ferrovia.movimento.modelo.MovimentoFerroviarioInterno;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MovimentoFerroviarioInternoRepositorio
        extends JpaRepository<MovimentoFerroviarioInterno, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MovimentoFerroviarioInterno> findOneById(Long id);

    List<MovimentoFerroviarioInterno> findByVisitaTrem_IdOrderByCriadoEmDesc(Long visitaTremId);

    Optional<MovimentoFerroviarioInterno>
            findFirstByVisitaTrem_IdAndReservaAtivaTrueAndInicioPlanejadoLessThanAndFimPlanejadoGreaterThanOrderByInicioPlanejadoAsc(
                    Long visitaTremId,
                    LocalDateTime fimPlanejado,
                    LocalDateTime inicioPlanejado);
}
