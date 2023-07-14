package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.dto.PerfilAcessoDTO;
import br.com.cloudport.servicoautenticacao.model.PerfilAcesso;
import br.com.cloudport.servicoautenticacao.repository.PerfilAcessoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerfilAcessoService {

    private final PerfilAcessoRepository perfilAcessoRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public PerfilAcessoService(PerfilAcessoRepository perfilAcessoRepository, ModelMapper modelMapper) {
        this.perfilAcessoRepository = perfilAcessoRepository;
        this.modelMapper = modelMapper;
    }

    public List<PerfilAcessoDTO> listarTodosPerfis() {
        List<PerfilAcesso> perfis = perfilAcessoRepository.findAll();
        return perfis.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PerfilAcessoDTO encontrarPerfilPorId(Long id) {
        PerfilAcesso perfil = perfilAcessoRepository.findById(id).orElse(null);
        if (perfil != null) {
            return convertToDTO(perfil);
        }
        return null;
    }

    public PerfilAcessoDTO salvarPerfil(PerfilAcessoDTO perfilAcessoDTO) {
        PerfilAcesso perfil = convertToEntity(perfilAcessoDTO);
        PerfilAcesso perfilSalvo = perfilAcessoRepository.save(perfil);
        return convertToDTO(perfilSalvo);
    }

    public void deletarPerfil(Long id) {
        perfilAcessoRepository.deleteById(id);
    }

    private PerfilAcessoDTO convertToDTO(PerfilAcesso perfil) {
        return modelMapper.map(perfil, PerfilAcessoDTO.class);
    }

    private PerfilAcesso convertToEntity(PerfilAcessoDTO perfilAcessoDTO) {
        return modelMapper.map(perfilAcessoDTO, PerfilAcesso.class);
    }
}
