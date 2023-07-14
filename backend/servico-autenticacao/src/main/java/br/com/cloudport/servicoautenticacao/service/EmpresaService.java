package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.dto.EmpresaDTO;
import br.com.cloudport.servicoautenticacao.model.Empresa;
import br.com.cloudport.servicoautenticacao.repository.EmpresaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository, ModelMapper modelMapper) {
        this.empresaRepository = empresaRepository;
        this.modelMapper = modelMapper;
    }

    public List<EmpresaDTO> listarTodasEmpresas() {
        List<Empresa> empresas = empresaRepository.findAll();
        return empresas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public EmpresaDTO encontrarEmpresaPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id).orElse(null);
        if (empresa != null) {
            return convertToDTO(empresa);
        }
        return null;
    }

    public EmpresaDTO salvarEmpresa(EmpresaDTO empresaDTO) {
        Empresa empresa = convertToEntity(empresaDTO);
        Empresa empresaSalva = empresaRepository.save(empresa);
        return convertToDTO(empresaSalva);
    }

    public void deletarEmpresa(Long id) {
        empresaRepository.deleteById(id);
    }

    private EmpresaDTO convertToDTO(Empresa empresa) {
        return modelMapper.map(empresa, EmpresaDTO.class);
    }

    private Empresa convertToEntity(EmpresaDTO empresaDTO) {
        return modelMapper.map(empresaDTO, Empresa.class);
    }
}
