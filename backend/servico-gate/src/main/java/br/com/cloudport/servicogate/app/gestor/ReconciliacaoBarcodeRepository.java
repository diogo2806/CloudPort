package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReconciliacaoBarcodeRepository extends JpaRepository<ReconciliacaoBarcode, Long> {

    List<ReconciliacaoBarcode> findByGatePassId(Long gatePassId);

    List<ReconciliacaoBarcode> findByTipoDesinconia(TipoDesincroniaBarcode tipo);

    List<ReconciliacaoBarcode> findByResolvidoEmIsNull();

    @Query("SELECT r FROM ReconciliacaoBarcode r " +
           "WHERE r.detectadoEm >= :dataInicio AND r.detectadoEm <= :dataFim " +
           "ORDER BY r.detectadoEm DESC")
    List<ReconciliacaoBarcode> findByDetectadoEmBetween(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT r FROM ReconciliacaoBarcode r " +
           "WHERE r.alertaEnviado = false AND r.resolvidoEm IS NULL " +
           "ORDER BY r.detectadoEm ASC")
    List<ReconciliacaoBarcode> findNaoResolvidosSemAlerta();

    Optional<ReconciliacaoBarcode> findByGatePassIdAndTipoDesinconia(
            Long gatePassId, TipoDesincroniaBarcode tipo);
}
