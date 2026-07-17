package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DocumentoAgendamentoRepository extends JpaRepository<DocumentoAgendamento, Long> {

    List<DocumentoAgendamento> findByAgendamentoId(Long agendamentoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentoAgendamento> findOneById(Long id);

    List<DocumentoAgendamento> findTop100ByStatusValidacaoOrderByUpdatedAtAsc(
            StatusValidacaoDocumento statusValidacao);

    List<DocumentoAgendamento> findTop100ByStatusValidacaoAndProcessamentoOcrIniciadoEmBeforeOrderByProcessamentoOcrIniciadoEmAsc(
            StatusValidacaoDocumento statusValidacao,
            LocalDateTime processamentoOcrIniciadoEm);

    List<DocumentoAgendamento> findTop100ByStatusValidacaoAndProximaTentativaOcrEmLessThanEqualAndTentativasOcrLessThanOrderByProximaTentativaOcrEmAsc(
            StatusValidacaoDocumento statusValidacao,
            LocalDateTime proximaTentativaOcrEm,
            Integer tentativasOcr);
}
