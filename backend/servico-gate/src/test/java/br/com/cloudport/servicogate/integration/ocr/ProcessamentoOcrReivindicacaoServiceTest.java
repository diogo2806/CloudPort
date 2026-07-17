package br.com.cloudport.servicogate.integration.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class ProcessamentoOcrReivindicacaoServiceTest {

    @Mock
    private DocumentoAgendamentoRepository documentoAgendamentoRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ProcessamentoOcrReivindicacaoService service;

    @BeforeEach
    void setUp() {
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        service = new ProcessamentoOcrReivindicacaoService(
                documentoAgendamentoRepository,
                transactionManager,
                Duration.ofMinutes(10),
                3);
    }

    @Test
    void deveImpedirSegundaReivindicacaoEnquantoLeaseEstiverAtivo() {
        DocumentoAgendamento documento = novoDocumento();
        when(documentoAgendamentoRepository.findOneById(10L)).thenReturn(Optional.of(documento));
        when(documentoAgendamentoRepository.saveAndFlush(documento)).thenReturn(documento);

        Optional<ProcessamentoOcrMensagem> primeira = service.reivindicar(10L);
        Optional<ProcessamentoOcrMensagem> segunda = service.reivindicar(10L);

        assertThat(primeira).isPresent();
        assertThat(primeira.get().getReivindicadoEm()).isNotNull();
        assertThat(primeira.get().getTentativa()).isEqualTo(1);
        assertThat(segunda).isEmpty();
        assertThat(documento.getStatusValidacao()).isEqualTo(StatusValidacaoDocumento.PROCESSANDO);
        verify(documentoAgendamentoRepository, times(1)).saveAndFlush(documento);
    }

    @Test
    void deveLiberarLeaseRejeitadoSemConsumirTentativa() {
        DocumentoAgendamento documento = novoDocumento();
        when(documentoAgendamentoRepository.findOneById(10L)).thenReturn(Optional.of(documento));
        when(documentoAgendamentoRepository.saveAndFlush(documento)).thenReturn(documento);
        ProcessamentoOcrMensagem mensagem = service.reivindicar(10L).orElseThrow(AssertionError::new);

        boolean liberado = service.liberarReivindicacao(mensagem, "Executor indisponível");

        assertThat(liberado).isTrue();
        assertThat(documento.getStatusValidacao()).isEqualTo(StatusValidacaoDocumento.PENDENTE);
        assertThat(documento.getTentativasOcr()).isZero();
        assertThat(documento.getProcessamentoOcrIniciadoEm()).isNull();
        assertThat(documento.getUltimoErroOcr()).isEqualTo("Executor indisponível");
    }

    private DocumentoAgendamento novoDocumento() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(20L);
        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setId(10L);
        documento.setAgendamento(agendamento);
        documento.setUrlDocumento("documentos/20/arquivo.png");
        documento.setStatusValidacao(StatusValidacaoDocumento.PENDENTE);
        documento.setTentativasOcr(0);
        return documento;
    }
}
