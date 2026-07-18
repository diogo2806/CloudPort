package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEquipamentoInventarioRepositorio extends JpaRepository<TipoEquipamentoInventario, Long> {

    Optional<TipoEquipamentoInventario> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<TipoEquipamentoInventario> findAllByOrderByCategoriaAscCodigoAsc();

    List<TipoEquipamentoInventario> findByGrupoEquivalenciaIgnoreCaseAndAtivoTrueOrderByCodigoAsc(String grupoEquivalencia);
}
