package br.com.cloudport.servicorail.ferrovia.movimento.repositorio;

import br.com.cloudport.servicorail.ferrovia.movimento.modelo.ReservaRecursoFerroviario;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.TipoRecursoFerroviario;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRecursoFerroviarioRepositorio
        extends JpaRepository<ReservaRecursoFerroviario, Long> {

    Optional<ReservaRecursoFerroviario>
            findFirstByTipoRecursoAndCodigoRecursoIgnoreCaseAndAtivoTrueAndInicioReservaLessThanAndFimReservaGreaterThanOrderByInicioReservaAsc(
                    TipoRecursoFerroviario tipoRecurso,
                    String codigoRecurso,
                    LocalDateTime fimReserva,
                    LocalDateTime inicioReserva);
}
