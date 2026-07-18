package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.ContagemInventarioFisico;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContagemInventarioFisicoRepositorio extends JpaRepository<ContagemInventarioFisico, Long> {

    List<ContagemInventarioFisico> findByStatusNotOrderByRegistradoEmDesc(
            ContagemInventarioFisico.StatusContagem status);

    List<ContagemInventarioFisico> findByStatusInOrderByRegistradoEmDesc(
            Collection<ContagemInventarioFisico.StatusContagem> status);

    List<ContagemInventarioFisico> findByLoteOrderByRegistradoEmAsc(String lote);
}
