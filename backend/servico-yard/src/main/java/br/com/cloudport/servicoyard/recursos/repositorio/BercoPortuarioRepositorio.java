package br.com.cloudport.servicoyard.recursos.repositorio;

import br.com.cloudport.servicoyard.recursos.entidade.BercoPortuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BercoPortuarioRepositorio extends JpaRepository<BercoPortuario, Long> {

    Optional<BercoPortuario> findByCodigoIgnoreCase(String codigo);

    List<BercoPortuario> findAllByOrderByCodigoAsc();
}
