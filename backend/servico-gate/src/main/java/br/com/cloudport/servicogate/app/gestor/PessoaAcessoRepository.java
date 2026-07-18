package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.PessoaAcesso;
import br.com.cloudport.servicogate.model.enums.SituacaoPessoaAcesso;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface PessoaAcessoRepository extends JpaRepository<PessoaAcesso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "0"))
    Optional<PessoaAcesso> findByDocumentoNormalizado(String documentoNormalizado);

    List<PessoaAcesso> findBySituacaoOrderByUltimoAcessoEmAsc(SituacaoPessoaAcesso situacao);
}
