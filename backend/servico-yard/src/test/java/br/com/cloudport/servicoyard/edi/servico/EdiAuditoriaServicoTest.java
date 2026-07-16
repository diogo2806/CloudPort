package br.com.cloudport.servicoyard.edi.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdiAuditoriaServicoTest {

    @Mock
    private ProcessamentoEdiRepositorio repositorio;

    private EdiAuditoriaServico servico;

    @BeforeEach
    void configurar() {
        servico = new EdiAuditoriaServico(repositorio, new EdiIdentificadorExtrator(), 3, 1_000L);
    }

    @Test
    void deveRegistrarRecepcaoComChaveEIdentificadoresNormalizados() throws Exception {
        String conteudo = "UNB+UNOA:2+SENDER+RECEIVER+260716:1200+CTRL-01'"
                + "UNH+MSG-01+BAPLIE:D:95B:UN'";
        ProcessamentoEdi persistido = processamento(conteudo);
        when(repositorio.findByChaveIdempotencia(anyString())).thenAnswer(invocacao -> {
            persistido.setChaveIdempotencia(invocacao.getArgument(0));
            return Optional.of(persistido);
        });

        ProcessamentoEdiRespostaDto resposta = servico.registrarRecebimento(
                TipoMensagemEdi.BAPLIE,
                conteudo,
                null,
                null,
                null,
                "corr-01"
        );

        assertEquals("CTRL-01", resposta.identificadorInterchange());
        assertEquals("MSG-01", resposta.identificadorMensagem());
        assertNotNull(resposta.chaveIdempotencia());
        verify(repositorio).inserirSeAusente(
                eq("BAPLIE"),
                eq(conteudo),
                isNull(),
                isNull(),
                eq("MSG-01"),
                eq("CORR-01"),
                eq("CTRL-01"),
                eq("MSG-01"),
                anyString(),
                eq(sha256(conteudo)),
                any(LocalDateTime.class)
        );
    }

    @Test
    void deveEnviarParaQuarentenaQuandoEsgotarTentativas() {
        ProcessamentoEdi processamento = processamento("UNH+1+BAPLIE:D:95B:UN'");
        processamento.setTentativa(2);
        when(repositorio.findById(10L)).thenReturn(Optional.of(processamento));

        servico.registrarFalha(10L, new IllegalStateException("indisponivel"), false);

        assertEquals(3, processamento.getTentativa());
        assertEquals(StatusProcessamentoEdi.QUARENTENA, processamento.getStatus());
        assertEquals(null, processamento.getProximaTentativaEm());
    }

    private ProcessamentoEdi processamento(String conteudo) {
        ProcessamentoEdi processamento = new ProcessamentoEdi();
        processamento.setTipoMensagem(TipoMensagemEdi.BAPLIE);
        processamento.setStatus(StatusProcessamentoEdi.RECEBIDO);
        processamento.setConteudoOriginal(conteudo);
        processamento.setIdentificadorInterchange("CTRL-01");
        processamento.setIdentificadorMensagem("MSG-01");
        processamento.setReferenciaMensagem("MSG-01");
        processamento.setHashConteudo(sha256(conteudo));
        processamento.setTentativa(0);
        return processamento;
    }

    private String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
