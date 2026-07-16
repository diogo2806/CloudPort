package br.com.cloudport.servicoyard.edi.repositorio;

import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ProcessamentoEdiRepositorio extends JpaRepository<ProcessamentoEdi, Long> {

    Page<ProcessamentoEdi> findByTipoMensagemAndStatus(
            TipoMensagemEdi tipoMensagem,
            StatusProcessamentoEdi status,
            Pageable pageable
    );

    Page<ProcessamentoEdi> findByTipoMensagem(TipoMensagemEdi tipoMensagem, Pageable pageable);

    Page<ProcessamentoEdi> findByStatus(StatusProcessamentoEdi status, Pageable pageable);

    Optional<ProcessamentoEdi> findByChaveIdempotencia(String chaveIdempotencia);

    Optional<ProcessamentoEdi> findTopByReprocessamentoDeIdOrderByTentativaDesc(Long reprocessamentoDeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProcessamentoEdi> findTop20ByStatusInAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(
            Collection<StatusProcessamentoEdi> status,
            LocalDateTime proximaTentativaEm
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProcessamentoEdi> findTop100ByStatusAndProcessandoDesdeBeforeOrderByProcessandoDesdeAsc(
            StatusProcessamentoEdi status,
            LocalDateTime processandoDesde
    );
}
