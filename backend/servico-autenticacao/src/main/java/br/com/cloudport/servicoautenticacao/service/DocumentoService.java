package br.com.cloudport.servicoautenticacao.service;

import br.com.cloudport.servicoautenticacao.model.Documento;
import br.com.cloudport.servicoautenticacao.repository.DocumentoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;

    @Autowired
    public DocumentoService(DocumentoRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    public List<Documento> listarTodosDocumentos() {
        return documentoRepository.findAll();
    }

    public Documento encontrarDocumentoPorId(Long id) {
        return documentoRepository.findById(id).orElse(null);
    }

    public Documento salvarDocumento(Documento documento) {
        return documentoRepository.save(documento);
    }

    public void deletarDocumento(Long id) {
        documentoRepository.deleteById(id);
    }
}
