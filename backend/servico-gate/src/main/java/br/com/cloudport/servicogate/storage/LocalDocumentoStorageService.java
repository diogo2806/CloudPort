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
        validarAgendamentoId(agendamentoId);

        try {
            Path baseDirectory = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
            Files.createDirectories(baseDirectory);

            Path agendamentoDirectory = resolverCaminhoSeguro(baseDirectory, String.valueOf(agendamentoId));
            Files.createDirectories(agendamentoDirectory);

            String originalFilename = arquivo.getOriginalFilename();
            String sanitizedExtension = extrairExtensao(originalFilename);
            String generatedName = UUID.randomUUID() + (sanitizedExtension.isEmpty() ? "" : "." + sanitizedExtension);
            Path destino = resolverCaminhoSeguro(agendamentoDirectory, generatedName);

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
        Path arquivo = resolverCaminhoSeguro(baseDirectory, storageKey);
        return new FileSystemResource(arquivo);
    }

    private Path resolverCaminhoSeguro(Path diretorioBase, String caminhoRelativo) {
        Path caminhoResolvido = diretorioBase.resolve(caminhoRelativo).normalize();
        if (!caminhoResolvido.startsWith(diretorioBase)) {
            throw new BusinessException("Caminho de documento inválido");
        }
        return caminhoResolvido;
    }

    private void validarAgendamentoId(Long agendamentoId) {
        if (agendamentoId == null || agendamentoId <= 0) {
            throw new BusinessException("Identificador de agendamento inválido");
        }
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
