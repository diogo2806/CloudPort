package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.PrefixoEquipamentoInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrefixoEquipamentoInventarioRepositorio extends JpaRepository<PrefixoEquipamentoInventario, Long> {

    Optional<PrefixoEquipamentoInventario> findByPrefixoIgnoreCase(String prefixo);

    boolean existsByPrefixoIgnoreCase(String prefixo);

    List<PrefixoEquipamentoInventario> findAllByOrderByPrefixoAsc();
}
