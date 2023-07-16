package br.com.cloudport.servicoautenticacao.repository;

import br.com.cloudport.servicoautenticacao.dto.SolicitacaoAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.model.StatusCadastro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitacaoAcessoService {

    private final SolicitacaoAcessoRepository solicitacaoAcessoRepository;

    @Autowired
    public SolicitacaoAcessoService(SolicitacaoAcessoRepository solicitacaoAcessoRepository) {
        this.solicitacaoAcessoRepository = solicitacaoAcessoRepository;
    }

    public SolicitacaoAcesso salvarSolicitacao(SolicitacaoAcessoDTO solicitacaoDTO) {
        SolicitacaoAcesso solicitacao = solicitacaoDTO.toModel();
        return solicitacaoAcessoRepository.save(solicitacao);
    }

    public List<SolicitacaoAcesso> listarSolicitacoesPendentes() {
        return solicitacaoAcessoRepository.findByStatus(StatusCadastro.PENDENTE);
    }

}
