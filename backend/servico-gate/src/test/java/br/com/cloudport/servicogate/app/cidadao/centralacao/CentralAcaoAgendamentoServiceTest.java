package br.com.cloudport.servicogate.app.cidadao.centralacao;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoService;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.CentralAcaoAgendamentoRespostaDTO;
import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CentralAcaoAgendamentoServiceTest {

    @Mock
    private AgendamentoService agendamentoService;

    @Mock
    private AutenticacaoClient autenticacaoClient;

    @Mock
    private ClienteStatusPatio clienteStatusPatio;

    @InjectMocks
    private CentralAcaoAgendamentoService service;

    @AfterEach
    void limparContextoSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarEstruturaVaziaQuandoNaoExistiremAgendamentos() {
        when(agendamentoService.buscar(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(clienteStatusPatio.consultarStatus(any())).thenReturn(Optional.empty());

        CentralAcaoAgendamentoRespostaDTO resposta = service.montarVisaoCompleta("Bearer token");

        assertNotNull(resposta);
        assertNotNull(resposta.getAgendamentos());
        assertTrue(resposta.getAgendamentos().isEmpty());
    }

    @Test
    void deveIsolarFalhaDaIntegracaoDePatio() {
        when(agendamentoService.buscar(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(clienteStatusPatio.consultarStatus(any()))
                .thenThrow(new IllegalStateException("yard indisponível"));

        CentralAcaoAgendamentoRespostaDTO resposta = service.montarVisaoCompleta("Bearer token");

        assertNotNull(resposta);
        assertTrue(resposta.getAgendamentos().isEmpty());
        assertNull(resposta.getSituacaoPatio());
    }

    @Test
    void deveRetornarListaVaziaParaTransportadoraAutorizadaSemVinculo() {
        when(agendamentoService.buscar(isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("Transportadora autenticada sem vínculo válido"));
        when(clienteStatusPatio.consultarStatus(any())).thenReturn(Optional.empty());

        CentralAcaoAgendamentoRespostaDTO resposta = service.montarVisaoCompleta("Bearer token");

        assertNotNull(resposta);
        assertTrue(resposta.getAgendamentos().isEmpty());
    }
}
