package br.com.cloudport.serviconavio.escala.repositorio;

import br.com.cloudport.serviconavio.escala.entidade.ProntidaoBerco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProntidaoBercoRepositorio extends JpaRepository<ProntidaoBerco, Long> {

    Optional<ProntidaoBerco> findTopByEscalaIdOrderByVersaoChecklistDesc(Long escalaId);

    List<ProntidaoBerco> findByEscalaIdOrderByVersaoChecklistDesc(Long escalaId);
}
