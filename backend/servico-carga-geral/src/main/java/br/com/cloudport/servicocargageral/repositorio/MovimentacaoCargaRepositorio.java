package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoCargaRepositorio extends JpaRepository<MovimentacaoCarga, UUID> {

    List<MovimentacaoCarga> findTop100ByOrderByOcorridoEmDesc();
}
