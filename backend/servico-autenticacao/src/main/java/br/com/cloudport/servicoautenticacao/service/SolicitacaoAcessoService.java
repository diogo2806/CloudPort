package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.dto.SolicitacaoAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.repository.SolicitacaoAcessoRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SolicitacaoAcessoService {

    private final SolicitacaoAcessoRepository solicitacaoAcessoRepository;

    @Autowired
    public SolicitacaoAcessoService(SolicitacaoAcessoRepository solicitacaoAcessoRepository) {
        this.solicitacaoAcessoRepository = solicitacaoAcessoRepository;
    }

    public SolicitacaoAcessoDTO salvarSolicitacao(SolicitacaoAcessoDTO solicitacaoAcessoDTO) {
        SolicitacaoAcesso solicitacaoAcesso = solicitacaoAcessoDTO.toModel();
        SolicitacaoAcesso solicitacaoSalva = solicitacaoAcessoRepository.save(solicitacaoAcesso);
        return new SolicitacaoAcessoDTO(solicitacaoSalva);
    }

    public void aprovarSolicitacao(Long id) {
    }

    public List<SolicitacaoAcesso> listarSolicitacoesPendentes() {
        return null;
    }
}
