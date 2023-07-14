package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Documento;
import br.com.cloudport.servicoautenticacao.dto.DocumentoDTO;
import br.com.cloudport.servicoautenticacao.repository.DocumentoRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public DocumentoService(DocumentoRepository documentoRepository, ModelMapper modelMapper) {
        this.documentoRepository = documentoRepository;
        this.modelMapper = modelMapper;
    }

    public List<DocumentoDTO> listarTodosDocumentos() {
        List<Documento> documentos = documentoRepository.findAll();
        return documentos.stream()
                .map(documento -> modelMapper.map(documento, DocumentoDTO.class))
                .collect(Collectors.toList());
    }

    public DocumentoDTO encontrarDocumentoPorId(Long id) {
        Documento documento = documentoRepository.findById(id).orElse(null);
        return documento != null ? modelMapper.map(documento, DocumentoDTO.class) : null;
    }

    public DocumentoDTO salvarDocumento(DocumentoDTO documentoDTO) {
        Documento documento = modelMapper.map(documentoDTO, Documento.class);
        Documento savedDocumento = documentoRepository.save(documento);
        return modelMapper.map(savedDocumento, DocumentoDTO.class);
    }

    public void deletarDocumento(Long id) {
        documentoRepository.deleteById(id);
    }
}
