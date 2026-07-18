package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusReservaGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoMovimentoGateCarga;
import br.com.cloudport.servicocargageral.dominio.ReservaGateCarga;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaGateCargaRepositorio extends JpaRepository<ReservaGateCarga, UUID> {

    Optional<ReservaGateCarga> findByCommandIdReserva(UUID commandIdReserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaGateCarga> findComBloqueioById(UUID id);

    List<ReservaGateCarga> findByAgendamentoCodigoOrderByReservadoEmDesc(String agendamentoCodigo);

    @Query("select coalesce(sum(r.quantidade), 0) from ReservaGateCarga r "
            + "where r.loteId = :loteId and r.tipoMovimento = :tipo and r.status = :status")
    BigDecimal somarQuantidadeReservada(
            @Param("loteId") UUID loteId,
            @Param("tipo") TipoMovimentoGateCarga tipo,
            @Param("status") StatusReservaGateCarga status);

    @Query("select coalesce(sum(r.volumeM3), 0) from ReservaGateCarga r "
            + "where r.loteId = :loteId and r.tipoMovimento = :tipo and r.status = :status")
    BigDecimal somarVolumeReservado(
            @Param("loteId") UUID loteId,
            @Param("tipo") TipoMovimentoGateCarga tipo,
            @Param("status") StatusReservaGateCarga status);

    @Query("select coalesce(sum(r.pesoKg), 0) from ReservaGateCarga r "
            + "where r.loteId = :loteId and r.tipoMovimento = :tipo and r.status = :status")
    BigDecimal somarPesoReservado(
            @Param("loteId") UUID loteId,
            @Param("tipo") TipoMovimentoGateCarga tipo,
            @Param("status") StatusReservaGateCarga status);
}
