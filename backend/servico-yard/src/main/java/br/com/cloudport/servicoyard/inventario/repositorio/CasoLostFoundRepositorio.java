package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.CasoLostFound;
import br.com.cloudport.servicoyard.inventario.modelo.StatusCasoLostFound;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CasoLostFoundRepositorio extends JpaRepository<CasoLostFound, Long> {

    Optional<CasoLostFound> findFirstByIdentificacaoLidaIgnoreCaseAndStatusIn(
            String identificacaoLida, Collection<StatusCasoLostFound> statuses);

    List<CasoLostFound> findAllByOrderByAbertoEmDesc();
}
