package br.com.cloudport.servicoyard.patio.custodia.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaComandoDto;
import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaRespostaDto;
import br.com.cloudport.servicoyard.patio.custodia.modelo.CustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.repositorio.CustodiaExchangeAreaRepositorio;
import br.com.cloudport.servicoyard.patio.servico.PublicadorEventoMovimentoPatio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustodiaExchangeAreaServicoTest {

    @Mock
    private CustodiaExchangeAreaRepositorio repositorio;

    @Mock
    private PublicadorEventoMovimentoPatio publicadorEvento;

    private CustodiaExchangeAreaServico servico;

    @BeforeEach
    void setUp() {
        servico = new CustodiaExchangeAreaServico(repositorio, publicadorEvento);
    }

    @Test
    void deveRegistrarEntregaUmaUnicaVez() {
        CustodiaExchangeAreaComandoDto comando = comando("ENTREGA-1", "LACRE-10,LACRE-20");
        when(repositorio.findByChaveIdempotenciaEntrega("ENTREGA-1")).thenReturn(Optional.empty());
        when(repositorio.findFirstByCodigoUnidadeIgnoreCaseAndStatusInOrderByCriadoEmDesc(
                any(), any())).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any())).thenAnswer(invocation -> {
            CustodiaExchangeArea custodia = invocation.getArgument(0);
            custodia.setId(10L);
            return custodia;
        });

        CustodiaExchangeAreaRespostaDto resposta = servico.entregarNaExchangeArea(comando);

        assertEquals(StatusCustodiaExchangeArea.ENTREGUE, resposta.getStatus());
        assertFalse(resposta.isBloqueada());
        assertEquals("LACRE-10,LACRE-20", resposta.getLacresEntrega());
        verify(publicadorEvento).entregarNaExchangeArea(any());
    }

    @Test
    void deveTransferirCustodiaQuandoConferenciaFisicaConfere() {
        CustodiaExchangeArea custodia = custodiaEntregue();
        CustodiaExchangeAreaComandoDto comando = comando("RECEBIMENTO-1", "LACRE-20 LACRE-10");
        when(repositorio.findByChaveIdempotenciaRecebimento("RECEBIMENTO-1")).thenReturn(Optional.empty());
        when(repositorio.findByIdForUpdate(10L)).thenReturn(Optional.of(custodia));
        when(repositorio.saveAndFlush(custodia)).thenReturn(custodia);

        CustodiaExchangeAreaRespostaDto resposta = servico.receberDaExchangeArea(10L, comando);

        assertEquals(StatusCustodiaExchangeArea.RECEBIDA, resposta.getStatus());
        assertFalse(resposta.isBloqueada());
        verify(publicadorEvento).receberDaExchangeArea(any());
    }

    @Test
    void deveBloquearCustodiaQuandoCondicaoOuLacreDivergir() {
        CustodiaExchangeArea custodia = custodiaEntregue();
        CustodiaExchangeAreaComandoDto comando = comando("RECEBIMENTO-2", "LACRE-99");
        comando.setCondicao("AVARIADO");
        when(repositorio.findByChaveIdempotenciaRecebimento("RECEBIMENTO-2")).thenReturn(Optional.empty());
        when(repositorio.findByIdForUpdate(10L)).thenReturn(Optional.of(custodia));
        when(repositorio.saveAndFlush(custodia)).thenReturn(custodia);

        CustodiaExchangeAreaRespostaDto resposta = servico.receberDaExchangeArea(10L, comando);

        assertEquals(StatusCustodiaExchangeArea.DIVERGENTE, resposta.getStatus());
        assertTrue(resposta.isBloqueada());
        assertTrue(resposta.getMotivoDivergencia().contains("condição divergente"));
        assertTrue(resposta.getMotivoDivergencia().contains("lacres divergente"));
        verify(publicadorEvento).receberDaExchangeArea(any());
    }

    private CustodiaExchangeAreaComandoDto comando(String chave, String lacres) {
        CustodiaExchangeAreaComandoDto comando = new CustodiaExchangeAreaComandoDto();
        comando.setCodigoUnidade("CONT-001");
        comando.setArea("EA-01");
        comando.setPosicao("P-03");
        comando.setEquipamento("TT-07");
        comando.setOperador("Operador teste");
        comando.setCondicao("INTEGRO");
        comando.setLacres(lacres);
        comando.setChaveIdempotencia(chave);
        return comando;
    }

    private CustodiaExchangeArea custodiaEntregue() {
        CustodiaExchangeArea custodia = new CustodiaExchangeArea();
        custodia.setId(10L);
        custodia.setCodigoUnidade("CONT-001");
        custodia.setArea("EA-01");
        custodia.setPosicao("P-03");
        custodia.setEquipamentoEntrega("TT-01");
        custodia.setOperadorEntrega("Operador entrega");
        custodia.setCondicaoEntrega("INTEGRO");
        custodia.setLacresEntrega("LACRE-10,LACRE-20");
        custodia.setEntregueEm(LocalDateTime.now().minusMinutes(2));
        custodia.setChaveIdempotenciaEntrega("ENTREGA-1");
        custodia.setStatus(StatusCustodiaExchangeArea.ENTREGUE);
        custodia.setBloqueada(false);
        return custodia;
    }
}
