package br.com.cloudport.servicoyard.patio.repositorio;

import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoPatioRepositorio extends JpaRepository<EquipamentoPatio, Long> {

    Optional<EquipamentoPatio> findByIdentificador(String identificador);

    List<EquipamentoPatio> findAllByOrderByTipoEquipamentoAscIdentificadorAsc();

    List<EquipamentoPatio> findByTipoEquipamentoAndStatusOperacional(TipoEquipamento tipoEquipamento,
                                                                      StatusEquipamento statusOperacional);
}
