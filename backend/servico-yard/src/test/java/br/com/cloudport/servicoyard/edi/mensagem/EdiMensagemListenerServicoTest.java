package br.com.cloudport.servicoyard.edi.mensagem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.servico.EdiAuditoriaServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdiMensagemListenerServicoTest {

    @Mock
    private EdiAuditoriaServico auditoria;

    private EdiMensagemListenerServico listener;

    @BeforeEach
    void configurar() {
        listener = new EdiMensagemListenerServico(auditoria);
    }

    @Test
    void deveRegistrarCoprarNaRecepcaoIdempotenteSemProcessarDiretamente() {
        CoprarMensagemDto dto = coprar();
        when(auditoria.registrarRecebimento(
                TipoMensagemEdi.COPRAR,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        )).thenReturn(resposta(101L, TipoMensagemEdi.COPRAR));

        listener.receberCoprar(dto);

        verify(auditoria).registrarRecebimento(
                TipoMensagemEdi.COPRAR,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        );
    }

    @Test
    void deveRegistrarCoarriNaRecepcaoIdempotenteSemProcessarDiretamente() {
        CoarriMensagemDto dto = coarri();
        when(auditoria.registrarRecebimento(
                TipoMensagemEdi.COARRI,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        )).thenReturn(resposta(102L, TipoMensagemEdi.COARRI));

        listener.receberCoarri(dto);

        verify(auditoria).registrarRecebimento(
                TipoMensagemEdi.COARRI,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        );
    }

    @Test
    void devePropagarFalhaDeRecepcaoParaImpedirConfirmacaoRabbitMq() {
        CoprarMensagemDto dto = coprar();
        IllegalStateException falha = new IllegalStateException("persistencia indisponivel");
        when(auditoria.registrarRecebimento(
                TipoMensagemEdi.COPRAR,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        )).thenThrow(falha);

        assertThrows(IllegalStateException.class, () -> listener.receberCoprar(dto));
    }

    private CoprarMensagemDto coprar() {
        CoprarMensagemDto dto = new CoprarMensagemDto();
        dto.setCodigoNavio("NAVIO-01");
        dto.setCodigoViagem("VIAGEM-01");
        dto.setReferenciaMensagem("COPRAR-01");
        dto.setConteudoEdifact("UNB+UNOA:2+SENDER+RECEIVER+260716:1200+CTRL-01'"
                + "UNH+COPRAR-01+COPRAR:D:95B:UN'");
        return dto;
    }

    private CoarriMensagemDto coarri() {
        CoarriMensagemDto dto = new CoarriMensagemDto();
        dto.setCodigoNavio("NAVIO-01");
        dto.setCodigoViagem("VIAGEM-01");
        dto.setReferenciaMensagem("COARRI-01");
        dto.setConteudoEdifact("UNB+UNOA:2+SENDER+RECEIVER+260716:1200+CTRL-02'"
                + "UNH+COARRI-01+COARRI:D:95B:UN'");
        return dto;
    }

    private ProcessamentoEdiRespostaDto resposta(Long id, TipoMensagemEdi tipo) {
        return new ProcessamentoEdiRespostaDto(
                id,
                tipo,
                StatusProcessamentoEdi.RECEBIDO,
                "NAVIO-01",
                "VIAGEM-01",
                null,
                null,
                null,
                "chave",
                "hash",
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null
        );
    }
}
