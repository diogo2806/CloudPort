package br.com.cloudport.servicorail.ferrovia.repositorio;

import br.com.cloudport.servicorail.ferrovia.modelo.ReplanejamentoConteinerFerroviario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplanejamentoConteinerFerroviarioRepositorio
        extends JpaRepository<ReplanejamentoConteinerFerroviario, Long> {

    List<ReplanejamentoConteinerFerroviario> findByVisitaTremIdOrderByCriadoEmDesc(Long visitaTremId);
}
