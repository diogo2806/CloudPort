package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Contato;
import br.com.cloudport.servicoautenticacao.repository.ContatoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContatoService {

    private final ContatoRepository contatoRepository;

    @Autowired
    public ContatoService(ContatoRepository contatorepository) {
        this.contatoRepository = contatorepository;
    }

    public List<Contato> listarTodosContatos() {
        return contatoRepository.findAll();
    }

    public Contato encontrarContatoPorId(Long id) {
        return contatoRepository.findById(id).orElse(null);
    }

    public Contato salvarContato(Contato contato) {
        return contatoRepository.save(contato);
    }

    public void deletarContato(Long id) {
        contatoRepository.deleteById(id);
    }
}
