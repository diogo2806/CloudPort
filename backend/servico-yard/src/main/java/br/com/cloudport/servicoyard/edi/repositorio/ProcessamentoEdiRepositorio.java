package br.com.cloudport.servicoyard.edi.repositorio;

import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessamentoEdiRepositorio extends JpaRepository<ProcessamentoEdi, Long> {

    Page<ProcessamentoEdi> findByTipoMensagemAndStatus(
            TipoMensagemEdi tipoMensagem,
            StatusProcessamentoEdi status,
            Pageable pageable
    );

    Page<ProcessamentoEdi> findByTipoMensagem(TipoMensagemEdi tipoMensagem, Pageable pageable);

    Page<ProcessamentoEdi> findByStatus(StatusProcessamentoEdi status, Pageable pageable);
}
