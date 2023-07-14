package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.AcessoSistema;
import br.com.cloudport.servicoautenticacao.repository.AcessoSistemaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcessoSistemaService {

    private final AcessoSistemaRepository acessoSistemaRepository;

    @Autowired
    public AcessoSistemaService(AcessoSistemaRepository acessoSistemaRepository) {
        this.acessoSistemaRepository = acessoSistemaRepository;
    }

    public List<AcessoSistema> listarTodosAcessos() {
        return acessoSistemaRepository.findAll();
    }

    public AcessoSistema encontrarAcessoPorId(Long id) {
        return acessoSistemaRepository.findById(id).orElse(null);
    }

    public AcessoSistema salvarAcesso(AcessoSistema acessoSistema) {
        return acessoSistemaRepository.save(acessoSistema);
    }

    public void deletarAcesso(Long id) {
        acessoSistemaRepository.deleteById(id);
    }
}
