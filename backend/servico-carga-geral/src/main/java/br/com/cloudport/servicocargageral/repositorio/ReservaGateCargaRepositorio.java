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

public interface ReservaGateCargaRepositorio extends JpaRepository<ReservaGateCarga, UUID> {

    Optional<ReservaGateCarga> findByCommandIdReserva(UUID commandIdReserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservaGateCarga> findComBloqueioById(UUID id);

    List<ReservaGateCarga> findByAgendamentoCodigoOrderByReservadoEmDesc(String agendamentoCodigo);

    List<ReservaGateCarga> findByLoteIdAndTipoMovimentoAndStatus(
            UUID loteId,
            TipoMovimentoGateCarga tipoMovimento,
            StatusReservaGateCarga status);

    default BigDecimal somarQuantidadeReservada(
            UUID loteId,
            TipoMovimentoGateCarga tipoMovimento,
            StatusReservaGateCarga status) {
        return findByLoteIdAndTipoMovimentoAndStatus(loteId, tipoMovimento, status).stream()
                .map(ReservaGateCarga::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarVolumeReservado(
            UUID loteId,
            TipoMovimentoGateCarga tipoMovimento,
            StatusReservaGateCarga status) {
        return findByLoteIdAndTipoMovimentoAndStatus(loteId, tipoMovimento, status).stream()
                .map(ReservaGateCarga::getVolumeM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal somarPesoReservado(
            UUID loteId,
            TipoMovimentoGateCarga tipoMovimento,
            StatusReservaGateCarga status) {
        return findByLoteIdAndTipoMovimentoAndStatus(loteId, tipoMovimento, status).stream()
                .map(ReservaGateCarga::getPesoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
