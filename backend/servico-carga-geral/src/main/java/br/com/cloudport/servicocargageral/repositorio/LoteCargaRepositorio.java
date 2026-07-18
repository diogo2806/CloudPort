package br.com.cloudport.servicocargageral.repositorio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.StatusOrdemFerroviariaCarga;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LoteCargaRepositorio extends JpaRepository<LoteCarga, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = {"item", "item.conhecimento"})
    Optional<LoteCarga> findByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"item", "item.conhecimento", "movimentacoes", "historicoCustodiaFerroviaria"})
    Optional<LoteCarga> findComBloqueioById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"item", "item.conhecimento", "historicoCustodiaFerroviaria"})
    Optional<LoteCarga> findByIdAndVisitaTremId(UUID id, String visitaTremId);

    @EntityGraph(attributePaths = {"item", "item.conhecimento"})
    List<LoteCarga> findAllByOrderByAtualizadoEmDesc();

    @EntityGraph(attributePaths = {"item", "item.conhecimento"})
    List<LoteCarga> findByStatusOrderByAtualizadoEmDesc(StatusLoteCarga status);

    @EntityGraph(attributePaths = {"item", "item.conhecimento"})
    List<LoteCarga> findByNaturezaOrderByAtualizadoEmDesc(NaturezaCarga natureza);

    @EntityGraph(attributePaths = {"item", "item.conhecimento"})
    List<LoteCarga> findByStatusAndNaturezaOrderByAtualizadoEmDesc(StatusLoteCarga status, NaturezaCarga natureza);

    @EntityGraph(attributePaths = {"item", "item.conhecimento", "historicoCustodiaFerroviaria"})
    List<LoteCarga> findByVisitaTremIdOrderBySequenciaFerroviariaAsc(String visitaTremId);

    @EntityGraph(attributePaths = {"item", "item.conhecimento", "historicoCustodiaFerroviaria"})
    List<LoteCarga> findByVisitaTremIdAndStatusOrdemFerroviariaOrderBySequenciaFerroviariaAsc(
            String visitaTremId,
            StatusOrdemFerroviariaCarga statusOrdemFerroviaria);
}
