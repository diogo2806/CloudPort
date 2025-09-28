package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.dto.dashboard.OcupacaoPorHoraDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByCodigo(String codigo);

    List<Agendamento> findByStatus(StatusAgendamento status);

    List<Agendamento> findByJanelaAtendimentoData(LocalDate data);

    @Query("SELECT new br.com.cloudport.servicogate.dto.dashboard.OcupacaoPorHoraDTO(" +
            "j.horaInicio, " +
            "SUM(CASE WHEN a.id IS NOT NULL AND a.status <> br.com.cloudport.servicogate.model.enums.StatusAgendamento.CANCELADO THEN 1 ELSE 0 END), " +
            "j.capacidade) " +
            "FROM JanelaAtendimento j LEFT JOIN j.agendamentos a " +
            "WHERE j.data = :data " +
            "GROUP BY j.horaInicio, j.capacidade " +
            "ORDER BY j.horaInicio")
    List<OcupacaoPorHoraDTO> calcularOcupacaoPorHora(@Param("data") LocalDate data);
}
