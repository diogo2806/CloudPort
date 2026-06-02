package br.com.cloudport.servicoyard.recursos.repositorio;

import br.com.cloudport.servicoyard.recursos.entidade.ZonaArmazenagem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaArmazenagemRepositorio extends JpaRepository<ZonaArmazenagem, Long> {

    Optional<ZonaArmazenagem> findByCodigoIgnoreCase(String codigo);

    List<ZonaArmazenagem> findAllByOrderByCodigoAsc();
}
