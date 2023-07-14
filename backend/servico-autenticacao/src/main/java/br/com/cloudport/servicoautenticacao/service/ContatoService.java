package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Contato;
import br.com.cloudport.servicoautenticacao.dto.ContatoDTO;
import br.com.cloudport.servicoautenticacao.repository.ContatoRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContatoService {

    private final ContatoRepository contatoRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ContatoService(ContatoRepository contatoRepository, ModelMapper modelMapper) {
        this.contatoRepository = contatoRepository;
        this.modelMapper = modelMapper;
    }

    public List<ContatoDTO> listarTodosContatos() {
        List<Contato> contatos = contatoRepository.findAll();
        return contatos.stream()
                .map(contato -> modelMapper.map(contato, ContatoDTO.class))
                .collect(Collectors.toList());
    }

    public ContatoDTO encontrarContatoPorId(Long id) {
        Contato contato = contatoRepository.findById(id).orElse(null);
        return contato != null ? modelMapper.map(contato, ContatoDTO.class) : null;
    }

    public ContatoDTO salvarContato(ContatoDTO contatoDTO) {
        Contato contato = modelMapper.map(contatoDTO, Contato.class);
        Contato savedContato = contatoRepository.save(contato);
        return modelMapper.map(savedContato, ContatoDTO.class);
    }

    public void deletarContato(Long id) {
        contatoRepository.deleteById(id);
    }
}
