package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Procuracao;
import br.com.cloudport.servicoautenticacao.repository.ProcuracaoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcuracaoService {

    private final ProcuracaoRepository procuracaoRepository;

    @Autowired
    public ProcuracaoService(ProcuracaoRepository procuracaoRepository) {
        this.procuracaoRepository = procuracaoRepository;
    }

    public List<Procuracao> listarTodasProcuracoes() {
        return procuracaoRepository.findAll();
    }

    public Procuracao encontrarProcuracaoPorId(Long id) {
        return procuracaoRepository.findById(id).orElse(null);
    }

    public Procuracao salvarProcuracao(Procuracao procuracao) {
        return procuracaoRepository.save(procuracao);
    }

    public void deletarProcuracao(Long id) {
        procuracaoRepository.deleteById(id);
    }
}
