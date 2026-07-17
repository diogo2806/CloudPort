package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.model.MovimentacaoPessoaAcesso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoPessoaAcessoRepository extends JpaRepository<MovimentacaoPessoaAcesso, Long> {

    Page<MovimentacaoPessoaAcesso> findAllByOrderByRegistradoEmDesc(Pageable pageable);

    Page<MovimentacaoPessoaAcesso> findByPessoa_DocumentoNormalizadoOrderByRegistradoEmDesc(
            String documentoNormalizado,
            Pageable pageable);
}
