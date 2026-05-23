package br.com.cloudport.serviconavio.escala.repositorio;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EscalaRepositorio extends JpaRepository<Escala, Long> {

    List<Escala> findByNavioIdentificadorOrderByChegadaPrevistaAsc(Long navioId);

    @Query("SELECT e FROM Escala e "
            + "WHERE e.chegadaPrevista BETWEEN :inicio AND :limite "
            + "AND e.fase NOT IN :fasesExcluidas "
            + "ORDER BY e.chegadaPrevista ASC")
    List<Escala> buscarCronograma(@Param("inicio") LocalDateTime inicio,
                                  @Param("limite") LocalDateTime limite,
                                  @Param("fasesExcluidas") List<FaseEscala> fasesExcluidas);
}
