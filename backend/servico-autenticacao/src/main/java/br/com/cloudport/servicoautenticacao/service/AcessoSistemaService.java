package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.AcessoSistema;
import br.com.cloudport.servicoautenticacao.dto.AcessoSistemaDTO;
import br.com.cloudport.servicoautenticacao.repository.AcessoSistemaRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcessoSistemaService {

    private final AcessoSistemaRepository acessoSistemaRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public AcessoSistemaService(AcessoSistemaRepository acessoSistemaRepository, ModelMapper modelMapper) {
        this.acessoSistemaRepository = acessoSistemaRepository;
        this.modelMapper = modelMapper;
    }

    public List<AcessoSistemaDTO> listarTodosAcessos() {
        List<AcessoSistema> acessos = acessoSistemaRepository.findAll();
        return acessos.stream()
                .map(acesso -> modelMapper.map(acesso, AcessoSistemaDTO.class))
                .collect(Collectors.toList());
    }

    public AcessoSistemaDTO encontrarAcessoPorId(Long id) {
        AcessoSistema acesso = acessoSistemaRepository.findById(id).orElse(null);
        return acesso != null ? modelMapper.map(acesso, AcessoSistemaDTO.class) : null;
    }

    public AcessoSistemaDTO salvarAcesso(AcessoSistemaDTO acessoSistemaDTO) {
        AcessoSistema acesso = modelMapper.map(acessoSistemaDTO, AcessoSistema.class);
        AcessoSistema savedAcesso = acessoSistemaRepository.save(acesso);
        return modelMapper.map(savedAcesso, AcessoSistemaDTO.class);
    }

    public void deletarAcesso(Long id) {
        acessoSistemaRepository.deleteById(id);
    }
}
