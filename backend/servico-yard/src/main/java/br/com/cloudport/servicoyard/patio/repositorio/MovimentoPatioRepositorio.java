package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoPatioRepositorio extends JpaRepository<MovimentoPatio, Long> {

    List<MovimentoPatio> findTop50ByOrderByRegistradoEmDesc();
}
