package br.com.cloudport.servicogate.integration.ocr;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProcessamentoOcrReivindicacaoService {

    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final TransactionTemplate requiresNew;
    private final Duration processamentoExpirado;
    private final int maxTentativas;

    public ProcessamentoOcrReivindicacaoService(
            DocumentoAgendamentoRepository documentoAgendamentoRepository,
            PlatformTransactionManager transactionManager,
            @Value("${cloudport.gate.ocr.processamento-expirado:PT10M}") Duration processamentoExpirado,
            @Value("${cloudport.gate.ocr.max-tentativas:3}") int maxTentativas) {
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.processamentoExpirado = processamentoExpirado;
        this.maxTentativas = maxTentativas;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Optional<ProcessamentoOcrMensagem> reivindicar(Long documentoId) {
        ProcessamentoOcrMensagem mensagem = requiresNew.execute(status -> reivindicarNaTransacao(documentoId));
        return Optional.ofNullable(mensagem);
    }

    public boolean liberarReivindicacao(ProcessamentoOcrMensagem mensagem, String motivo) {
        Boolean liberado = requiresNew.execute(status -> liberarNaTransacao(mensagem, motivo));
        return Boolean.TRUE.equals(liberado);
    }

    private ProcessamentoOcrMensagem reivindicarNaTransacao(Long documentoId) {
        DocumentoAgendamento documento = documentoAgendamentoRepository.findOneById(documentoId).orElse(null);
        if (documento == null) {
            return null;
        }

        LocalDateTime agora = agoraNormalizado();
        if (!estaElegivel(documento, agora)) {
            return null;
        }

        int tentativas = tentativas(documento);
        documento.setTentativasOcr(tentativas + 1);
        documento.setStatusValidacao(StatusValidacaoDocumento.PROCESSANDO);
        documento.setMensagemValidacao("Documento em análise pelo OCR...");
        documento.setUltimoErroOcr(null);
        documento.setProximaTentativaOcrEm(null);
        documento.setProcessamentoOcrIniciadoEm(agora);
        documentoAgendamentoRepository.saveAndFlush(documento);
        return ProcessamentoOcrMensagem.from(documento);
    }

    private boolean liberarNaTransacao(ProcessamentoOcrMensagem mensagem, String motivo) {
        if (mensagem == null || mensagem.getDocumentoId() == null || mensagem.getReivindicadoEm() == null) {
            return false;
        }

        DocumentoAgendamento documento = documentoAgendamentoRepository
                .findOneById(mensagem.getDocumentoId())
                .orElse(null);
        if (!correspondeReivindicacaoAtual(documento, mensagem)) {
            return false;
        }

        documento.setTentativasOcr(Math.max(tentativas(documento) - 1, 0));
        documento.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
        documento.setMensagemValidacao("Documento aguardando nova tentativa de processamento OCR.");
        documento.setUltimoErroOcr(motivo);
        documento.setProximaTentativaOcrEm(null);
        documento.setProcessamentoOcrIniciadoEm(null);
        documentoAgendamentoRepository.saveAndFlush(documento);
        return true;
    }

    private boolean estaElegivel(DocumentoAgendamento documento, LocalDateTime agora) {
        if (tentativas(documento) >= maxTentativas) {
            return false;
        }

        StatusValidacaoDocumento status = documento.getStatusValidacao();
        if (status == StatusValidacaoDocumento.PENDENTE) {
            return true;
        }
        if (status == StatusValidacaoDocumento.PROCESSANDO) {
            LocalDateTime iniciadoEm = documento.getProcessamentoOcrIniciadoEm();
            return iniciadoEm == null || !iniciadoEm.isAfter(agora.minus(processamentoExpirado));
        }
        if (status == StatusValidacaoDocumento.FALHA) {
            LocalDateTime proximaTentativa = documento.getProximaTentativaOcrEm();
            return proximaTentativa != null && !proximaTentativa.isAfter(agora);
        }
        return false;
    }

    private boolean correspondeReivindicacaoAtual(
            DocumentoAgendamento documento,
            ProcessamentoOcrMensagem mensagem) {
        return documento != null
                && documento.getStatusValidacao() == StatusValidacaoDocumento.PROCESSANDO
                && Objects.equals(documento.getProcessamentoOcrIniciadoEm(), mensagem.getReivindicadoEm())
                && (mensagem.getTentativa() == null
                || Objects.equals(documento.getTentativasOcr(), mensagem.getTentativa()));
    }

    private int tentativas(DocumentoAgendamento documento) {
        return documento.getTentativasOcr() == null ? 0 : documento.getTentativasOcr();
    }

    private LocalDateTime agoraNormalizado() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
