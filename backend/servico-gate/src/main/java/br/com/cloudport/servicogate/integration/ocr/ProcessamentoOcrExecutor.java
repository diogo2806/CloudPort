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
import java.util.Objects;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProcessamentoOcrExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessamentoOcrExecutor.class);

    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final DocumentoStorageService documentoStorageService;
    private final AgendamentoRealtimeService agendamentoRealtimeService;
    private final OcrIntegrationProperties properties;
    private final TransactionTemplate requiresNew;
    private final Duration retryDelay;
    private final int maxTentativas;

    public ProcessamentoOcrExecutor(DocumentoAgendamentoRepository documentoAgendamentoRepository,
                                     DocumentoStorageService documentoStorageService,
                                     AgendamentoRealtimeService agendamentoRealtimeService,
                                     OcrIntegrationProperties properties,
                                     PlatformTransactionManager transactionManager,
                                     @Value("${cloudport.gate.ocr.retry-atraso:PT1M}") Duration retryDelay,
                                     @Value("${cloudport.gate.ocr.max-tentativas:3}") int maxTentativas) {
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.documentoStorageService = documentoStorageService;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
        this.properties = properties;
        this.retryDelay = retryDelay;
        this.maxTentativas = maxTentativas;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void processar(ProcessamentoOcrMensagem mensagem) {
        ContextoOcr contexto = carregarReivindicacao(mensagem);
        if (contexto == null) {
            return;
        }
        notificarAtualizacao(contexto.agendamento);

        try {
            validarImagem(contexto.chaveArmazenamento, properties.getTempoMaximoProcessamento());
            ContextoOcr concluido = concluirProcessamento(contexto);
            if (concluido == null) {
                return;
            }
            notificarAtualizacao(concluido.agendamento);
            agendamentoRealtimeService.notificarDocumentosRevalidados(concluido.agendamento);
            LOGGER.info("event=ocr.validado documentoId={} agendamentoId={} tentativa={}",
                    contexto.documentoId, contexto.agendamento.getId(), contexto.tentativa);
        } catch (BusinessException ex) {
            ContextoOcr falha = registrarFalha(contexto, ex.getMessage(), false);
            if (falha == null) {
                return;
            }
            notificarAtualizacao(falha.agendamento);
            LOGGER.warn("event=ocr.falha-permanente documentoId={} tentativa={} mensagem={}",
                    contexto.documentoId, contexto.tentativa, ex.getMessage());
        } catch (IOException | RuntimeException ex) {
            String mensagemFalha = "Falha transitória ao acessar o arquivo para validação OCR.";
            ContextoOcr falha = registrarFalha(contexto, mensagemFalha, true);
            if (falha == null) {
                return;
            }
            notificarAtualizacao(falha.agendamento);
            LOGGER.error("event=ocr.falha-transitoria documentoId={} tentativa={}",
                    contexto.documentoId, contexto.tentativa, ex);
        }
    }

    private ContextoOcr carregarReivindicacao(ProcessamentoOcrMensagem mensagem) {
        return requiresNew.execute(status -> {
            DocumentoAgendamento documento = documentoAgendamentoRepository.findOneById(mensagem.getDocumentoId())
                    .orElseThrow(() -> new NotFoundException("Documento para OCR não encontrado"));
            if (!correspondeReivindicacaoAtual(documento, mensagem.getReivindicadoEm(), mensagem.getTentativa())) {
                return null;
            }
            inicializarDocumentos(documento.getAgendamento());
            return contexto(documento);
        });
    }

    private ContextoOcr concluirProcessamento(ContextoOcr contexto) {
        return requiresNew.execute(status -> {
            DocumentoAgendamento documento = documentoAgendamentoRepository.findOneById(contexto.documentoId)
                    .orElseThrow(() -> new NotFoundException("Documento para OCR não encontrado"));
            if (!correspondeReivindicacaoAtual(documento, contexto.reivindicadoEm, contexto.tentativa)) {
                return null;
            }
            documento.setStatusValidacao(StatusValidacaoDocumento.VALIDADO);
            documento.setUltimaRevalidacao(LocalDateTime.now());
            documento.setMensagemValidacao("Documento validado automaticamente via OCR.");
            documento.setUltimoErroOcr(null);
            documento.setProximaTentativaOcrEm(null);
            documento.setProcessamentoOcrIniciadoEm(null);
            documentoAgendamentoRepository.saveAndFlush(documento);
            inicializarDocumentos(documento.getAgendamento());
            return contexto(documento);
        });
    }

    private ContextoOcr registrarFalha(ContextoOcr contexto, String mensagem, boolean retryable) {
        return requiresNew.execute(status -> {
            DocumentoAgendamento documento = documentoAgendamentoRepository.findOneById(contexto.documentoId)
                    .orElseThrow(() -> new NotFoundException("Documento para OCR não encontrado"));
            if (!correspondeReivindicacaoAtual(documento, contexto.reivindicadoEm, contexto.tentativa)) {
                return null;
            }
            documento.setStatusValidacao(StatusValidacaoDocumento.FALHA);
            documento.setMensagemValidacao(mensagem);
            documento.setUltimoErroOcr(mensagem);
            documento.setProcessamentoOcrIniciadoEm(null);
            int tentativas = documento.getTentativasOcr() == null ? 0 : documento.getTentativasOcr();
            documento.setProximaTentativaOcrEm(retryable && tentativas < maxTentativas
                    ? LocalDateTime.now().plus(retryDelay)
                    : null);
            documentoAgendamentoRepository.saveAndFlush(documento);
            inicializarDocumentos(documento.getAgendamento());
            return contexto(documento);
        });
    }

    private boolean correspondeReivindicacaoAtual(
            DocumentoAgendamento documento,
            LocalDateTime reivindicadoEm,
            Integer tentativa) {
        return reivindicadoEm != null
                && documento.getStatusValidacao() == StatusValidacaoDocumento.PROCESSANDO
                && Objects.equals(documento.getProcessamentoOcrIniciadoEm(), reivindicadoEm)
                && (tentativa == null || Objects.equals(documento.getTentativasOcr(), tentativa));
    }

    private ContextoOcr contexto(DocumentoAgendamento documento) {
        return new ContextoOcr(
                documento.getId(),
                documento.getUrlDocumento(),
                documento.getTentativasOcr() == null ? 0 : documento.getTentativasOcr(),
                documento.getProcessamentoOcrIniciadoEm(),
                documento.getAgendamento());
    }

    private void validarImagem(String chaveArmazenamento, Duration timeout) throws IOException {
        Resource recurso = documentoStorageService.carregarComoResource(chaveArmazenamento);
        if (!recurso.exists()) {
            throw new BusinessException("Arquivo do documento não encontrado para validação.");
        }
        try (InputStream inputStream = recurso.getInputStream()) {
            long inicio = System.nanoTime();
            BufferedImage imagem = ImageIO.read(inputStream);
            Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);
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

    private void inicializarDocumentos(Agendamento agendamento) {
        if (agendamento != null && agendamento.getDocumentos() != null) {
            agendamento.getDocumentos().size();
        }
    }

    private void notificarAtualizacao(Agendamento agendamento) {
        if (agendamento != null) {
            agendamentoRealtimeService.notificarDocumentosAtualizados(agendamento);
        }
    }

    private static final class ContextoOcr {
        private final Long documentoId;
        private final String chaveArmazenamento;
        private final int tentativa;
        private final LocalDateTime reivindicadoEm;
        private final Agendamento agendamento;

        private ContextoOcr(Long documentoId,
                            String chaveArmazenamento,
                            int tentativa,
                            LocalDateTime reivindicadoEm,
                            Agendamento agendamento) {
            this.documentoId = documentoId;
            this.chaveArmazenamento = chaveArmazenamento;
            this.tentativa = tentativa;
            this.reivindicadoEm = reivindicadoEm;
            this.agendamento = agendamento;
        }
    }
}
