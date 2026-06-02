package br.com.cloudport.servicoyard.recursos.repositorio;

import br.com.cloudport.servicoyard.recursos.entidade.EquipamentoBerco;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoBercoRepositorio extends JpaRepository<EquipamentoBerco, Long> {

    List<EquipamentoBerco> findAllByOrderByBercoCodigoAscIdentificadorAsc();
}
