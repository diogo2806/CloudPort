package br.com.cloudport.servicogate.storage;

import br.com.cloudport.servicogate.config.DocumentoStorageProperties;
import br.com.cloudport.servicogate.exception.BusinessException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalDocumentoStorageService implements DocumentoStorageService {

    private final DocumentoStorageProperties properties;

    public LocalDocumentoStorageService(DocumentoStorageProperties properties) {
        this.properties = properties;
        if (!"local".equalsIgnoreCase(properties.getProvider())) {
            throw new BusinessException("Provedor de armazenamento não suportado: " + properties.getProvider());
        }
    }

    @Override
    public StoredDocumento armazenar(Long agendamentoId, MultipartFile arquivo) {
        try {
            Path baseDirectory = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
            Files.createDirectories(baseDirectory);
            Path agendamentoDirectory = baseDirectory.resolve(String.valueOf(agendamentoId));
            Files.createDirectories(agendamentoDirectory);

            String originalFilename = arquivo.getOriginalFilename();
            String sanitizedExtension = extrairExtensao(originalFilename);
            String generatedName = UUID.randomUUID() + (sanitizedExtension.isEmpty() ? "" : "." + sanitizedExtension);
            Path destino = agendamentoDirectory.resolve(generatedName);

            try {
                Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ioException) {
                throw new UncheckedIOException(ioException);
            }

            String storageKey = agendamentoId + "/" + generatedName;
            String nomeOriginal = StringUtils.hasText(originalFilename) ? originalFilename : generatedName;
            return new StoredDocumento(storageKey, nomeOriginal, arquivo.getContentType(), arquivo.getSize());
        } catch (IOException e) {
            throw new BusinessException("Erro ao armazenar documento de agendamento", e);
        }
    }

    @Override
    public Resource carregarComoResource(String storageKey) {
        Path baseDirectory = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
        Path arquivo = baseDirectory.resolve(storageKey).normalize();
        if (!arquivo.startsWith(baseDirectory)) {
            throw new BusinessException("Caminho de documento inválido");
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public boolean exists(String storageKey) {
        Path baseDirectory = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
        Path arquivo = baseDirectory.resolve(storageKey).normalize();
        if (!arquivo.startsWith(baseDirectory)) {
            return false;
        }
        return Files.exists(arquivo);
    }

    private String extrairExtensao(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String sanitized = originalFilename.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]+", "-");
        int lastDot = sanitized.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return sanitized.substring(lastDot + 1);
    }
}
