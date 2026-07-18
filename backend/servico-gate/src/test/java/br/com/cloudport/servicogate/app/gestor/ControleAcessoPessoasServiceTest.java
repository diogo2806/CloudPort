package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.EntradaPessoaRequest;
import br.com.cloudport.servicogate.app.gestor.dto.MovimentacaoPessoaDTO;
import br.com.cloudport.servicogate.app.gestor.dto.SaidaPessoaRequest;
import br.com.cloudport.servicogate.model.MovimentacaoPessoaAcesso;
import br.com.cloudport.servicogate.model.PessoaAcesso;
import br.com.cloudport.servicogate.model.enums.DirecaoMovimentacaoPessoa;
import br.com.cloudport.servicogate.model.enums.SituacaoPessoaAcesso;
import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControleAcessoPessoasServiceTest {

    private static final String MENSAGEM_CONFLITO_CONCORRENCIA =
            "A transição de acesso já foi realizada por outra requisição. Atualize os dados e tente novamente.";

    @Mock
    private PessoaAcessoRepository pessoaAcessoRepository;

    @Mock
    private MovimentacaoPessoaAcessoRepository movimentacaoRepository;

    @InjectMocks
    private ControleAcessoPessoasService service;

    @Test
    void registrarEntrada_criaPessoaEHistorico() {
        EntradaPessoaRequest request = novaEntrada();

        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.empty());
        when(pessoaAcessoRepository.saveAndFlush(any(PessoaAcesso.class))).thenAnswer(invocation -> {
            PessoaAcesso pessoa = invocation.getArgument(0);
            pessoa.setId(10L);
            return pessoa;
        });
        when(movimentacaoRepository.saveAndFlush(any(MovimentacaoPessoaAcesso.class))).thenAnswer(invocation -> {
            MovimentacaoPessoaAcesso movimentacao = invocation.getArgument(0);
            movimentacao.setId(20L);
            return movimentacao;
        });

        MovimentacaoPessoaDTO resultado = service.registrarEntrada(request);

        assertEquals(20L, resultado.id());
        assertEquals(10L, resultado.pessoaId());
        assertEquals(DirecaoMovimentacaoPessoa.ENTRADA, resultado.direcao());
        assertEquals("123.456.789-00", resultado.documento());
        assertEquals("SISTEMA", resultado.usuarioResponsavel());
        assertNotNull(resultado.registradoEm());

        ArgumentCaptor<PessoaAcesso> pessoaCaptor = ArgumentCaptor.forClass(PessoaAcesso.class);
        verify(pessoaAcessoRepository).saveAndFlush(pessoaCaptor.capture());
        assertEquals(SituacaoPessoaAcesso.DENTRO, pessoaCaptor.getValue().getSituacao());
        assertEquals("12345678900", pessoaCaptor.getValue().getDocumentoNormalizado());
    }

    @Test
    void registrarEntrada_rejeitaSegundaEntradaAberta() {
        PessoaAcesso pessoa = pessoaDentro();
        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.of(pessoa));

        EntradaPessoaRequest request = new EntradaPessoaRequest(
                "Maria da Silva",
                "12345678900",
                TipoPessoaAcesso.VISITANTE,
                null,
                null,
                "Portaria principal",
                null,
                null,
                null,
                null);

        assertThrows(ResponseStatusException.class, () -> service.registrarEntrada(request));
        verify(pessoaAcessoRepository, never()).saveAndFlush(any(PessoaAcesso.class));
        verify(movimentacaoRepository, never()).saveAndFlush(any(MovimentacaoPessoaAcesso.class));
    }

    @Test
    void registrarEntrada_converteColisaoDoDocumentoEmConflito() {
        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.empty());
        ConstraintViolationException causa = new ConstraintViolationException(
                "documento duplicado",
                new SQLException("duplicate key"),
                "uk_pessoa_acesso_documento_normalizado");
        when(pessoaAcessoRepository.saveAndFlush(any(PessoaAcesso.class)))
                .thenThrow(new DataIntegrityViolationException("documento duplicado", causa));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarEntrada(novaEntrada()));

        assertEquals(409, exception.getRawStatusCode());
        assertEquals(MENSAGEM_CONFLITO_CONCORRENCIA, exception.getReason());
        verify(movimentacaoRepository, never()).saveAndFlush(any(MovimentacaoPessoaAcesso.class));
    }

    @Test
    void registrarSaida_calculaPermanenciaEFechaEntrada() {
        PessoaAcesso pessoa = pessoaDentro();
        pessoa.setUltimoAcessoEm(LocalDateTime.now().minusMinutes(45));
        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.of(pessoa));
        when(pessoaAcessoRepository.saveAndFlush(any(PessoaAcesso.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movimentacaoRepository.saveAndFlush(any(MovimentacaoPessoaAcesso.class))).thenAnswer(invocation -> {
            MovimentacaoPessoaAcesso movimentacao = invocation.getArgument(0);
            movimentacao.setId(30L);
            return movimentacao;
        });

        MovimentacaoPessoaDTO resultado = service.registrarSaida(new SaidaPessoaRequest(
                "123.456.789-00",
                "Portaria principal",
                "Saída normal",
                "PORTAL_CLOUDPORT_REACT",
                "corr-2",
                null));

        assertEquals(DirecaoMovimentacaoPessoa.SAIDA, resultado.direcao());
        assertEquals(SituacaoPessoaAcesso.FORA, pessoa.getSituacao());
        assertNotNull(resultado.permanenciaMinutos());
        assertTrue(resultado.permanenciaMinutos() >= 44L);
    }

    @Test
    void registrarSaida_rejeitaPessoaSemEntradaAberta() {
        PessoaAcesso pessoa = pessoaDentro();
        pessoa.setSituacao(SituacaoPessoaAcesso.FORA);
        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.of(pessoa));

        SaidaPessoaRequest request = new SaidaPessoaRequest(
                "12345678900",
                "Portaria principal",
                null,
                null,
                null,
                null);

        assertThrows(ResponseStatusException.class, () -> service.registrarSaida(request));
        verify(movimentacaoRepository, never()).saveAndFlush(any(MovimentacaoPessoaAcesso.class));
    }

    @Test
    void registrarSaida_converteFalhaDeVersaoEmConflito() {
        PessoaAcesso pessoa = pessoaDentro();
        when(pessoaAcessoRepository.findByDocumentoNormalizado("12345678900")).thenReturn(Optional.of(pessoa));
        when(pessoaAcessoRepository.saveAndFlush(any(PessoaAcesso.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(PessoaAcesso.class, pessoa.getId()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrarSaida(new SaidaPessoaRequest(
                        "12345678900",
                        "Portaria principal",
                        null,
                        null,
                        null,
                        null)));

        assertEquals(409, exception.getRawStatusCode());
        assertEquals(MENSAGEM_CONFLITO_CONCORRENCIA, exception.getReason());
        verify(movimentacaoRepository, never()).saveAndFlush(any(MovimentacaoPessoaAcesso.class));
    }

    private EntradaPessoaRequest novaEntrada() {
        return new EntradaPessoaRequest(
                "Maria da Silva",
                "123.456.789-00",
                TipoPessoaAcesso.VISITANTE,
                "Empresa Exemplo",
                "CR-100",
                "Portaria principal",
                "Reunião operacional",
                "PORTAL_CLOUDPORT_REACT",
                "corr-1",
                "usuario-informado");
    }

    private PessoaAcesso pessoaDentro() {
        PessoaAcesso pessoa = new PessoaAcesso();
        pessoa.setId(10L);
        pessoa.setNome("Maria da Silva");
        pessoa.setDocumento("123.456.789-00");
        pessoa.setDocumentoNormalizado("12345678900");
        pessoa.setTipoPessoa(TipoPessoaAcesso.VISITANTE);
        pessoa.setSituacao(SituacaoPessoaAcesso.DENTRO);
        pessoa.setUltimoAcessoEm(LocalDateTime.now().minusMinutes(10));
        pessoa.setUltimoPontoAcesso("Portaria principal");
        return pessoa;
    }
}
