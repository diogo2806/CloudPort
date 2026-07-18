package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.DivergenciaPosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusDivergenciaPosicao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivergenciaPosicaoPatioRepositorio extends JpaRepository<DivergenciaPosicaoPatio, Long> {

    Optional<DivergenciaPosicaoPatio> findFirstByUnidadeIdAndStatusIn(
            Long unidadeId, Collection<StatusDivergenciaPosicao> statuses);

    List<DivergenciaPosicaoPatio> findAllByOrderByAbertaEmDesc();
}
