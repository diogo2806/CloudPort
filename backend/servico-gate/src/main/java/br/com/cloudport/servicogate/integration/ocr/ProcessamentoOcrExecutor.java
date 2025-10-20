package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.config.OcrIntegrationProperties;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import br.com.cloudport.servicogate.storage.DocumentoStorageService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessamentoOcrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrExecutor.class);

    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final DocumentoStorageService documentoStorageService;
    private final AgendamentoRealtimeService agendamentoRealtimeService;
    private final OcrIntegrationProperties properties;

    public ProcessamentoOcrExecutor(DocumentoAgendamentoRepository documentoAgendamentoRepository,
                                    DocumentoStorageService documentoStorageService,
                                    AgendamentoRealtimeService agendamentoRealtimeService,
                                    OcrIntegrationProperties properties) {
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.documentoStorageService = documentoStorageService;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
        this.properties = properties;
    }

    @Transactional
    public void processar(ProcessamentoOcrMensagem mensagem) {
        DocumentoAgendamento documento = documentoAgendamentoRepository.findById(mensagem.getDocumentoId())
                .orElseThrow(() -> new NotFoundException("Documento para OCR não encontrado"));
        documento.setStatusValidacao(StatusValidacaoDocumento.PROCESSANDO);
        documento.setMensagemValidacao("Documento em análise pelo OCR...");
        documentoAgendamentoRepository.save(documento);
        notificarAtualizacao(documento.getAgendamento());

        try {
            validarImagem(documento, properties.getTempoMaximoProcessamento());
            documento.setStatusValidacao(StatusValidacaoDocumento.VALIDADO);
            documento.setUltimaRevalidacao(LocalDateTime.now());
            documento.setMensagemValidacao("Documento validado automaticamente via OCR.");
            documentoAgendamentoRepository.save(documento);
            notificarAtualizacao(documento.getAgendamento());
            notificarRevalidacao(documento.getAgendamento());
            LOGGER.info("event=ocr.validado documentoId={} agendamentoId={}",
                    documento.getId(),
                    documento.getAgendamento() != null ? documento.getAgendamento().getId() : null);
        } catch (BusinessException ex) {
            LOGGER.warn("event=ocr.falha documentoId={} mensagem={}", documento.getId(), ex.getMessage());
            documento.setStatusValidacao(StatusValidacaoDocumento.FALHA);
            documento.setMensagemValidacao(ex.getMessage());
            documentoAgendamentoRepository.save(documento);
            notificarAtualizacao(documento.getAgendamento());
        } catch (IOException ex) {
            LOGGER.error("event=ocr.erro-leitura documentoId={}", documento.getId(), ex);
            documento.setStatusValidacao(StatusValidacaoDocumento.FALHA);
            documento.setMensagemValidacao("Não foi possível ler a imagem para validação.");
            documentoAgendamentoRepository.save(documento);
            notificarAtualizacao(documento.getAgendamento());
            throw new BusinessException("Falha ao acessar o arquivo para validação.", ex);
        }
    }

    private void validarImagem(DocumentoAgendamento documento, Duration timeout) throws IOException {
        Resource recurso = documentoStorageService.carregarComoResource(documento.getUrlDocumento());
        if (!recurso.exists()) {
            throw new BusinessException("Arquivo do documento não encontrado para validação.");
        }
        try (InputStream inputStream = recurso.getInputStream()) {
            long inicio = System.nanoTime();
            BufferedImage imagem = ImageIO.read(inputStream);
            long tempoProcessamento = System.nanoTime() - inicio;
            Duration duracao = Duration.ofNanos(tempoProcessamento);
            if (duracao.compareTo(timeout) > 0) {
                throw new BusinessException("Tempo limite excedido ao processar a imagem para OCR.");
            }
            if (imagem == null) {
                throw new BusinessException("O arquivo enviado não é uma imagem válida.");
            }
            if (imagem.getWidth() < 60 || imagem.getHeight() < 60) {
                throw new BusinessException("A imagem deve ter pelo menos 60x60 pixels.");
            }
        }
    }

    private void notificarAtualizacao(Agendamento agendamento) {
        if (agendamento != null) {
            agendamentoRealtimeService.notificarDocumentosAtualizados(agendamento);
        }
    }

    private void notificarRevalidacao(Agendamento agendamento) {
        if (agendamento != null) {
            agendamentoRealtimeService.notificarDocumentosRevalidados(agendamento);
        }
    }
}
