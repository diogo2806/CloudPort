package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.repository.projection.TempoMedioPermanenciaProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GatePassRepository extends JpaRepository<GatePass, Long> {

    Optional<GatePass> findByCodigo(String codigo);

    List<GatePass> findByStatus(StatusGate status);

    @Query(value = "SELECT DATE(gp.data_entrada) AS dia, " +
            "AVG(EXTRACT(EPOCH FROM (gp.data_saida - gp.data_entrada))/60) AS tempoMedioMinutos " +
            "FROM gate_pass gp " +
            "WHERE gp.data_entrada IS NOT NULL AND gp.data_saida IS NOT NULL " +
            "GROUP BY DATE(gp.data_entrada) " +
            "ORDER BY dia",
            nativeQuery = true)
    List<TempoMedioPermanenciaProjection> calcularTempoMedioPermanencia();
}
