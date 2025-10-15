package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoAgendamentoRepository extends JpaRepository<DocumentoAgendamento, Long> {

    List<DocumentoAgendamento> findByAgendamentoId(Long agendamentoId);
}
