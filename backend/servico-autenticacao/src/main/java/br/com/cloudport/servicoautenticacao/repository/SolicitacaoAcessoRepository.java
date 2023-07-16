package br.com.cloudport.servicoautenticacao.repository;

import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.model.StatusCadastro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitacaoAcessoRepository extends JpaRepository<SolicitacaoAcesso, Long> {
    // Metodos adicionais de pesquisa podem ser adicionados aqui
    List<SolicitacaoAcesso> findByStatus(StatusCadastro status);

}
