package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.dto.ProcuracaoDTO;
import br.com.cloudport.servicoautenticacao.model.Procuracao;
import br.com.cloudport.servicoautenticacao.repository.ProcuracaoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcuracaoService {

    private final ProcuracaoRepository procuracaoRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ProcuracaoService(ProcuracaoRepository procuracaoRepository, ModelMapper modelMapper) {
        this.procuracaoRepository = procuracaoRepository;
        this.modelMapper = modelMapper;
    }

    public List<ProcuracaoDTO> listarTodasProcuracoes() {
        List<Procuracao> procuracoes = procuracaoRepository.findAll();
        return procuracoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProcuracaoDTO encontrarProcuracaoPorId(Long id) {
        Procuracao procuracao = procuracaoRepository.findById(id).orElse(null);
        if (procuracao != null) {
            return convertToDTO(procuracao);
        }
        return null;
    }

    public ProcuracaoDTO salvarProcuracao(ProcuracaoDTO procuracaoDTO) {
        Procuracao procuracao = convertToEntity(procuracaoDTO);
        Procuracao procuracaoSalva = procuracaoRepository.save(procuracao);
        return convertToDTO(procuracaoSalva);
    }

    public void deletarProcuracao(Long id) {
        procuracaoRepository.deleteById(id);
    }

    private ProcuracaoDTO convertToDTO(Procuracao procuracao) {
        return modelMapper.map(procuracao, ProcuracaoDTO.class);
    }

    private Procuracao convertToEntity(ProcuracaoDTO procuracaoDTO) {
        return modelMapper.map(procuracaoDTO, Procuracao.class);
    }
}
