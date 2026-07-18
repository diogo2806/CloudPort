package br.com.cloudport.servicoyard.inventario.repositorio;

import br.com.cloudport.servicoyard.inventario.modelo.VinculoEquipamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoEquipamentoRepositorio extends JpaRepository<VinculoEquipamento, Long> {

    boolean existsByUnidadePrincipalIdAndUnidadeRelacionadaIdAndAtivoTrue(Long unidadePrincipalId,
                                                                          Long unidadeRelacionadaId);

    Optional<VinculoEquipamento> findByIdAndAtivoTrue(Long id);

    List<VinculoEquipamento> findByUnidadePrincipalIdOrUnidadeRelacionadaIdOrderByMontadoEmDesc(
            Long unidadePrincipalId,
            Long unidadeRelacionadaId);
}
