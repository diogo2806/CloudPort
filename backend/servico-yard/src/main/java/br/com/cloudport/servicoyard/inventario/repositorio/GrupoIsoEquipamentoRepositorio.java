package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.GrupoIsoEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoIsoEquipamentoRepositorio extends JpaRepository<GrupoIsoEquipamento, Long> {
    Optional<GrupoIsoEquipamento> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    List<GrupoIsoEquipamento> findAllByOrderByCodigoAsc();
    List<GrupoIsoEquipamento> findByCategoriaAndAtivoTrueOrderByCodigoAsc(TipoEquipamentoInventario.CategoriaEquipamento categoria);
}
