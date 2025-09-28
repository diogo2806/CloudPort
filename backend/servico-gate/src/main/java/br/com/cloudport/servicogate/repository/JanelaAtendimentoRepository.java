package br.com.cloudport.servicogate.repository;

import br.com.cloudport.servicogate.model.JanelaAtendimento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JanelaAtendimentoRepository extends JpaRepository<JanelaAtendimento, Long> {

    List<JanelaAtendimento> findByDataOrderByHoraInicio(LocalDate data);
}
