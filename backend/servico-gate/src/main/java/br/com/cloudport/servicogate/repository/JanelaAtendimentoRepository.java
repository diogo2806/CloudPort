package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.model.JanelaAtendimento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JanelaAtendimentoRepository extends JpaRepository<JanelaAtendimento, Long> {

    List<JanelaAtendimento> findByDataOrderByHoraInicio(LocalDate data);

    Page<JanelaAtendimento> findByData(LocalDate data, Pageable pageable);

    Page<JanelaAtendimento> findByDataBetween(LocalDate inicio, LocalDate fim, Pageable pageable);

    Page<JanelaAtendimento> findByDataGreaterThanEqual(LocalDate inicio, Pageable pageable);

    Page<JanelaAtendimento> findByDataLessThanEqual(LocalDate fim, Pageable pageable);
}
