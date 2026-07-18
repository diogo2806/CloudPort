package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeInventarioRepositorio extends JpaRepository<UnidadeInventario, Long> {

    Optional<UnidadeInventario> findByIdentificacaoIgnoreCase(String identificacao);

    boolean existsByIdentificacaoIgnoreCase(String identificacao);

    List<UnidadeInventario> findAllByOrderByIdentificacaoAsc();
}
