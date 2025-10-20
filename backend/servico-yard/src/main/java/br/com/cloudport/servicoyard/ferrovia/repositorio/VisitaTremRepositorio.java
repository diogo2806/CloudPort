package br.com.cloudport.servicoyard.ferrovia.repositorio;

import br.com.cloudport.servicoyard.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitaTremRepositorio extends JpaRepository<VisitaTrem, Long> {

    @Query("SELECT v FROM VisitaTrem v "
            + "WHERE (v.horaChegadaPrevista BETWEEN :inicio AND :limite) "
            + "OR ((v.statusVisita <> :statusFinalizado) "
            + "AND (v.horaPartidaPrevista IS NULL OR v.horaPartidaPrevista >= :referenciaAtiva) "
            + "AND v.horaChegadaPrevista <= :limite) "
            + "ORDER BY v.horaChegadaPrevista ASC, v.id ASC")
    List<VisitaTrem> buscarVisitasPlanejadasOuAtivas(@Param("inicio") LocalDateTime inicio,
                                                     @Param("referenciaAtiva") LocalDateTime referenciaAtiva,
                                                     @Param("limite") LocalDateTime limite,
                                                     @Param("statusFinalizado") StatusVisitaTrem statusFinalizado);

    @Query("SELECT DISTINCT v FROM VisitaTrem v "
            + "LEFT JOIN FETCH v.listaDescarga "
            + "LEFT JOIN FETCH v.listaCarga "
            + "WHERE v.id = :id")
    Optional<VisitaTrem> buscarPorIdComListas(@Param("id") Long id);
}
