package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.PessoaAcesso;
import br.com.cloudport.servicogate.model.enums.SituacaoPessoaAcesso;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PessoaAcessoRepository extends JpaRepository<PessoaAcesso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PessoaAcesso> findByDocumentoNormalizado(String documentoNormalizado);

    List<PessoaAcesso> findBySituacaoOrderByUltimoAcessoEmAsc(SituacaoPessoaAcesso situacao);
}
