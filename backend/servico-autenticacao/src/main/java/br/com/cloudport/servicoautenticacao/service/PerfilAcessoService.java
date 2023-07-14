package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.PerfilAcesso;
import br.com.cloudport.servicoautenticacao.repository.PerfilAcessoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerfilAcessoService {

    private final PerfilAcessoRepository perfilAcessoRepository;

    @Autowired
    public PerfilAcessoService(PerfilAcessoRepository perfilAcessoRepository) {
        this.perfilAcessoRepository = perfilAcessoRepository;
    }

    public List<PerfilAcesso> listarTodosPerfis() {
        return perfilAcessoRepository.findAll();
    }

    public PerfilAcesso encontrarPerfilPorId(Long id) {
        return perfilAcessoRepository.findById(id).orElse(null);
    }

    public PerfilAcesso salvarPerfil(PerfilAcesso perfilAcesso) {
        return perfilAcessoRepository.save(perfilAcesso);
    }

    public void deletarPerfil(Long id) {
        perfilAcessoRepository.deleteById(id);
    }
}
