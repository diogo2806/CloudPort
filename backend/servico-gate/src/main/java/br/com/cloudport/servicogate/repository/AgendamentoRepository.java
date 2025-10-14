package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.dto.dashboard.OcupacaoPorHoraDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.repository.projection.DashboardMetricsProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByCodigo(String codigo);

    Optional<Agendamento> findFirstByVeiculoPlacaIgnoreCaseAndStatusInOrderByHorarioPrevistoChegadaAsc(
            String placa,
            List<StatusAgendamento> status);

    Optional<Agendamento> findFirstByVeiculoIdAndStatusInOrderByHorarioPrevistoChegadaDesc(
            Long veiculoId,
            List<StatusAgendamento> status);

    List<Agendamento> findByStatus(StatusAgendamento status);

    List<Agendamento> findByStatusInOrderByHorarioPrevistoChegadaAsc(List<StatusAgendamento> status);

    List<Agendamento> findByJanelaAtendimentoData(LocalDate data);

    Page<Agendamento> findByJanelaAtendimentoData(LocalDate data, Pageable pageable);

    Page<Agendamento> findByJanelaAtendimentoDataBetween(LocalDate inicio, LocalDate fim, Pageable pageable);

    Page<Agendamento> findByJanelaAtendimentoDataGreaterThanEqual(LocalDate inicio, Pageable pageable);

    Page<Agendamento> findByJanelaAtendimentoDataLessThanEqual(LocalDate fim, Pageable pageable);

    long countByJanelaAtendimentoIdAndStatusNot(Long janelaAtendimentoId, StatusAgendamento status);

    long countByJanelaAtendimentoIdAndStatusNotAndIdNot(Long janelaAtendimentoId, StatusAgendamento status,
                                                        Long id);

    @Query("SELECT new br.com.cloudport.servicogate.dto.dashboard.OcupacaoPorHoraDTO(" +
            "j.horaInicio, " +
            "SUM(CASE WHEN a.id IS NOT NULL AND a.status <> br.com.cloudport.servicogate.model.enums.StatusAgendamento.CANCELADO THEN 1 ELSE 0 END), " +
            "j.capacidade) " +
            "FROM JanelaAtendimento j LEFT JOIN j.agendamentos a " +
            "WHERE j.data = :data " +
            "GROUP BY j.horaInicio, j.capacidade " +
            "ORDER BY j.horaInicio")
    List<OcupacaoPorHoraDTO> calcularOcupacaoPorHora(@Param("data") LocalDate data);

    @Query(value = "SELECT " +
            "COUNT(*) FILTER (WHERE a.status <> 'CANCELADO') AS total_agendamentos, " +
            "SUM(CASE WHEN a.status <> 'CANCELADO' AND a.horario_real_chegada IS NOT NULL AND a.horario_previsto_chegada IS NOT NULL " +
            "AND ABS(EXTRACT(EPOCH FROM (a.horario_real_chegada - a.horario_previsto_chegada))) <= (:toleranciaPontualidade * 60) THEN 1 ELSE 0 END) AS pontuais, " +
            "SUM(CASE WHEN a.status = 'NO_SHOW' THEN 1 ELSE 0 END) AS no_show, " +
            "AVG(EXTRACT(EPOCH FROM (a.horario_real_saida - a.horario_real_chegada)) / 60) FILTER (WHERE a.horario_real_chegada IS NOT NULL AND a.horario_real_saida IS NOT NULL) AS turnaround_medio, " +
            "COALESCE(COUNT(*) FILTER (WHERE a.status <> 'CANCELADO')::decimal / NULLIF(( " +
            "SELECT SUM(j.capacidade) FROM janela_atendimento j WHERE EXISTS ( " +
            "SELECT 1 FROM agendamento a2 WHERE a2.janela_atendimento_id = j.id " +
            "AND a2.status <> 'CANCELADO' " +
            "AND (:inicio IS NULL OR a2.horario_previsto_chegada >= :inicio) " +
            "AND (:fim IS NULL OR a2.horario_previsto_saida <= :fim) " +
            "AND (:transportadoraId IS NULL OR a2.transportadora_id = :transportadoraId) " +
            "AND (:tipoOperacao IS NULL OR a2.tipo_operacao = :tipoOperacao))), 0), 0) AS ocupacao_slots " +
            "FROM agendamento a " +
            "WHERE (:inicio IS NULL OR a.horario_previsto_chegada >= :inicio) " +
            "AND (:fim IS NULL OR a.horario_previsto_saida <= :fim) " +
            "AND (:transportadoraId IS NULL OR a.transportadora_id = :transportadoraId) " +
            "AND (:tipoOperacao IS NULL OR a.tipo_operacao = :tipoOperacao)",
            nativeQuery = true)
    DashboardMetricsProjection calcularMetricasDashboard(@Param("inicio") java.time.LocalDateTime inicio,
                                                         @Param("fim") java.time.LocalDateTime fim,
                                                         @Param("transportadoraId") Long transportadoraId,
                                                         @Param("tipoOperacao") String tipoOperacao,
                                                         @Param("toleranciaPontualidade") int toleranciaPontualidade);

    @Query("SELECT a FROM Agendamento a " +
            "JOIN FETCH a.transportadora t " +
            "JOIN FETCH a.janelaAtendimento j " +
            "LEFT JOIN FETCH a.gatePass gp " +
            "WHERE (:inicio IS NULL OR a.horarioPrevistoChegada >= :inicio) " +
            "AND (:fim IS NULL OR a.horarioPrevistoSaida <= :fim) " +
            "AND (:transportadoraId IS NULL OR t.id = :transportadoraId) " +
            "AND (:tipoOperacao IS NULL OR a.tipoOperacao = :tipoOperacao) " +
            "AND a.status <> br.com.cloudport.servicogate.model.enums.StatusAgendamento.CANCELADO " +
            "ORDER BY a.horarioPrevistoChegada ASC")
    List<Agendamento> buscarRelatorio(@Param("inicio") java.time.LocalDateTime inicio,
                                      @Param("fim") java.time.LocalDateTime fim,
                                      @Param("transportadoraId") Long transportadoraId,
                                      @Param("tipoOperacao") TipoOperacao tipoOperacao);
}
