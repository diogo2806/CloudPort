package br.com.cloudport.servicogate.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentoStorageService {

    StoredDocumento armazenar(Long agendamentoId, MultipartFile arquivo);

    Resource carregarComoResource(String storageKey);
}
