package br.com.cloudport.servicorail.ferrovia.locomotiva.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfiguracaoLocomotivaVisitaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfirmacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.EntregaCustodiaLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.LiberacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.PlanejamentoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.ModalidadeEmbarqueLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.StatusTransferenciaLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.TransferenciaLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.repositorio.TransferenciaLocomotivaRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.TipoVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransferenciaLocomotivaServicoTest {

    @Mock
    private TransferenciaLocomotivaRepositorio transferenciaRepositorio;

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    private TransferenciaLocomotivaServico servico;

    @BeforeEach
    void configurar() {
        servico = new TransferenciaLocomotivaServico(
                transferenciaRepositorio,
                visitaTremRepositorio,
                new SanitizadorEntrada(),
                Optional.empty());
    }

    @Test
    void deveConfigurarAPropriaVisitaComoLocomotiva() {
        VisitaTrem visita = visitaTrem(1L);
        when(visitaTremRepositorio.buscarPorIdComListas(1L)).thenReturn(Optional.of(visita));
        when(transferenciaRepositorio.findById(1L)).thenReturn(Optional.empty());
        salvarRetornandoEntidade();

        ConfiguracaoLocomotivaVisitaDto dto = configuracaoLocomotiva();

        assertEquals(StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA,
                servico.configurarVisitaComoLocomotiva(1L, dto).getStatus());
        assertEquals(TipoVisitaTrem.LOCOMOTIVA_ISOLADA, visita.getTipoVisita());
        assertEquals("LOCO-9001", servico.configurarVisitaComoLocomotiva(1L, dto).getIdentificadorTrem());
    }

    @Test
    void deveExecutarCustodiaPlanejamentoLiberacaoEEmbarquePelaVisita() {
        TransferenciaLocomotiva transferencia = transferencia();
        when(transferenciaRepositorio.findById(10L)).thenReturn(Optional.of(transferencia));
        salvarRetornandoEntidade();

        EntregaCustodiaLocomotivaDto entrega = new EntregaCustodiaLocomotivaDto();
        entrega.setNomeMaquinista("João Operador");
        entrega.setDocumentoEntrega("TERMO-2026-001");
        entrega.setResponsavelTerminal("Maria Terminal");
        servico.registrarEntregaCustodia(10L, entrega);
        assertEquals(StatusTransferenciaLocomotiva.SOB_CUSTODIA_TERMINAL, transferencia.getStatus());

        PlanejamentoEmbarqueLocomotivaDto planejamento = new PlanejamentoEmbarqueLocomotivaDto();
        planejamento.setVisitaNavioId(77L);
        planejamento.setCodigoVisitaNavio("VV-2026-077");
        planejamento.setModalidadeEmbarque(ModalidadeEmbarqueLocomotiva.RORO_REBOCADA);
        planejamento.setDeckPlanejado("DECK 3");
        planejamento.setPosicaoPlanejada("LINHA 02 / POSIÇÃO 04");
        servico.planejarEmbarque(10L, planejamento);
        assertEquals(StatusTransferenciaLocomotiva.PLANEJADA_PARA_EMBARQUE, transferencia.getStatus());

        LiberacaoEmbarqueLocomotivaDto liberacao = checklistCompleto();
        servico.liberarEmbarque(10L, liberacao);
        assertEquals(StatusTransferenciaLocomotiva.PRONTA_PARA_EMBARQUE, transferencia.getStatus());

        ConfirmacaoEmbarqueLocomotivaDto confirmacao = new ConfirmacaoEmbarqueLocomotivaDto();
        confirmacao.setPosicaoReal("DECK 3 / LINHA 02 / POSIÇÃO 04");
        servico.confirmarEmbarque(10L, confirmacao);
        assertEquals(StatusTransferenciaLocomotiva.EMBARCADA, transferencia.getStatus());
        assertEquals(StatusVisitaTrem.CONCLUIDO, transferencia.getVisitaTrem().getStatusVisita());
    }

    @Test
    void deveBloquearVisitaComVagaoPorqueALocomotivaEhOProprioTrem() {
        VisitaTrem visita = visitaTrem(1L);
        visita.getListaVagoes().add(new VagaoVisita(1, "VAG-001", "PLATAFORMA"));
        when(visitaTremRepositorio.buscarPorIdComListas(1L)).thenReturn(Optional.of(visita));

        assertThrows(ResponseStatusException.class,
                () -> servico.configurarVisitaComoLocomotiva(1L, configuracaoLocomotiva()));
    }

    @Test
    void deveBloquearLiberacaoComChecklistIncompleto() {
        TransferenciaLocomotiva transferencia = transferencia();
        transferencia.setStatus(StatusTransferenciaLocomotiva.PLANEJADA_PARA_EMBARQUE);
        when(transferenciaRepositorio.findById(10L)).thenReturn(Optional.of(transferencia));

        LiberacaoEmbarqueLocomotivaDto liberacao = checklistCompleto();
        liberacao.setPlanoAmarracaoAprovado(false);

        assertThrows(ResponseStatusException.class,
                () -> servico.liberarEmbarque(10L, liberacao));
    }

    private ConfiguracaoLocomotivaVisitaDto configuracaoLocomotiva() {
        ConfiguracaoLocomotivaVisitaDto dto = new ConfiguracaoLocomotivaVisitaDto();
        dto.setPesoToneladas(new BigDecimal("128.500"));
        dto.setComprimentoMetros(new BigDecimal("20.300"));
        dto.setLarguraMetros(new BigDecimal("3.100"));
        dto.setAlturaMetros(new BigDecimal("4.600"));
        return dto;
    }

    private void salvarRetornandoEntidade() {
        when(transferenciaRepositorio.save(any(TransferenciaLocomotiva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private LiberacaoEmbarqueLocomotivaDto checklistCompleto() {
        LiberacaoEmbarqueLocomotivaDto dto = new LiberacaoEmbarqueLocomotivaDto();
        dto.setFreioEstacionamentoAplicado(true);
        dto.setBateriasIsoladas(true);
        dto.setCombustivelProtegido(true);
        dto.setCalcosInstalados(true);
        dto.setPlanoAmarracaoAprovado(true);
        return dto;
    }

    private TransferenciaLocomotiva transferencia() {
        TransferenciaLocomotiva transferencia = new TransferenciaLocomotiva();
        transferencia.setId(10L);
        transferencia.setVisitaTrem(visitaTrem(10L));
        transferencia.setPesoToneladas(new BigDecimal("128.500"));
        transferencia.setComprimentoMetros(new BigDecimal("20.300"));
        transferencia.setLarguraMetros(new BigDecimal("3.100"));
        transferencia.setAlturaMetros(new BigDecimal("4.600"));
        transferencia.setStatus(StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA);
        return transferencia;
    }

    private VisitaTrem visitaTrem(Long id) {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(id);
        visita.setIdentificadorTrem("LOCO-9001");
        visita.setOperadoraFerroviaria("Ferrovia Teste");
        visita.setTipoVisita(TipoVisitaTrem.COMPOSICAO_FERROVIARIA);
        visita.setStatusVisita(StatusVisitaTrem.CHEGOU);
        return visita;
    }
}
