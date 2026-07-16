package br.com.cloudport.serviconaviosiderurgico.repositorio;

import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoGuindasteVisita;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanoGuindasteVisitaRepositorio extends JpaRepository<PlanoGuindasteVisita, Long> {
    List<PlanoGuindasteVisita> findByVisitaNavioIdOrderBySequenciaAsc(Long visitaNavioId);

    @Modifying
    @Query("delete from PlanoGuindasteVisita plano where plano.visitaNavio.id = :visitaNavioId")
    void deleteByVisitaNavioId(@Param("visitaNavioId") Long visitaNavioId);
}
