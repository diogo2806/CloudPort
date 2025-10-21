package br.com.cloudport.servicoyard.patio.listatrabalho.repositorio;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemTrabalhoPatioRepositorio extends JpaRepository<OrdemTrabalhoPatio, Long> {

    List<OrdemTrabalhoPatio> findByStatusOrdemInOrderByCriadoEmAsc(List<StatusOrdemTrabalhoPatio> status);

    boolean existsByCodigoConteinerIgnoreCaseAndStatusOrdemIn(String codigoConteiner,
                                                              List<StatusOrdemTrabalhoPatio> status);

    Optional<OrdemTrabalhoPatio> findByIdAndStatusOrdemIn(Long id, List<StatusOrdemTrabalhoPatio> status);
}
