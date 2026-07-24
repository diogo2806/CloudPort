package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.avisoestivagem.servico.AvisoEstivagemPatioServico;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRequest;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.TipoAcaoFisicaPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoTransferenciaFisicaServicoTest {

    @Mock
    private WorkQueuePatioRepositorio workQueueRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;
    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;
    @Mock
    private PosicaoPatioRepositorio posicaoRepositorio;
    @Mock
    private MapaPatioServico mapaPatioServico;
    @Mock
    private AvisoEstivagemPatioServico avisoEstivagemServico;

    private ConfirmacaoTransferenciaFisicaServico servico;

    @BeforeEach
    void configurar() {
        servico = new ConfirmacaoTransferenciaFisicaServico(
                workQueueRepositorio,
                ordemRepositorio,
                equipamentoRepositorio,
                conteinerRepositorio,
                posicaoRepositorio,
                mapaPatioServico,
                avisoEstivagemServico);
    }

    @Test
    void deveConfirmarGroundingComUnidadeCheDestinoESequenciaValidos() {
        OrdemTrabalhoPatio ordem = ordemGrounding();
        WorkQueuePatio fila = fila();
        EquipamentoPatio equipamento = equipamento();
        EventoVmtWorkInstructionRequest request = requestGrounding();
        ConteinerPatio persistido = new ConteinerPatio();
        persistido.setCodigo("CONT-10");

        when(workQueueRepositorio.findById(20L)).thenReturn(Optional.of(fila));
        when(equipamentoRepositorio.findById(30L)).thenReturn(Optional.of(equipamento));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(20L))
                .thenReturn(List.of(ordem));
        when(conteinerRepositorio.findByCodigoIgnoreCase("CONT-10"))
                .thenReturn(Optional.empty(), Optional.of(persistido));

        servico.confirmar(ordem, request);

        verify(mapaPatioServico).registrarOuAtualizarConteiner(any(ConteinerPatioRequisicaoDto.class));
        verify(avisoEstivagemServico).revalidarInventario("VMT_ORDEM_10");
        assertEquals(persistido, ordem.getConteiner());
    }

    @Test
    void deveRejeitarUnidadeLidaDivergenteAntesDeAlterarInventario() {
        OrdemTrabalhoPatio ordem = ordemGrounding();
        EventoVmtWorkInstructionRequest request = requestGrounding();
        request.setCodigoUnidadeLido("OUTRA-UNIDADE");

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> servico.confirmar(ordem, request));

        assertEquals(HttpStatus.CONFLICT, excecao.getStatus());
        verify(mapaPatioServico, never()).registrarOuAtualizarConteiner(any());
        verify(conteinerRepositorio, never()).save(any());
        verify(avisoEstivagemServico, never()).revalidarInventario(any());
    }

    @Test
    void deveRejeitarConclusaoForaDaSequenciaDaJobList() {
        OrdemTrabalhoPatio anterior = ordemGrounding();
        anterior.setId(9L);
        anterior.setStatusOrdem(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
        OrdemTrabalhoPatio ordem = ordemGrounding();
        WorkQueuePatio fila = fila();

        when(workQueueRepositorio.findById(20L)).thenReturn(Optional.of(fila));
        when(equipamentoRepositorio.findById(30L)).thenReturn(Optional.of(equipamento()));
        when(ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(20L))
                .thenReturn(List.of(anterior, ordem));

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> servico.confirmar(ordem, requestGrounding()));

        assertEquals(HttpStatus.CONFLICT, excecao.getStatus());
        verify(mapaPatioServico, never()).registrarOuAtualizarConteiner(any());
        verify(avisoEstivagemServico, never()).revalidarInventario(any());
    }

    private OrdemTrabalhoPatio ordemGrounding() {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(10L);
        ordem.setWorkQueueId(20L);
        ordem.setCodigoConteiner("CONT-10");
        ordem.setDestino("BLOCO-A");
        ordem.setLinhaDestino(1);
        ordem.setColunaDestino(2);
        ordem.setCamadaDestino("T1");
        ordem.setTipoMovimento(TipoMovimentoPatio.ALOCACAO);
        ordem.setStatusConteinerDestino(StatusConteiner.ARMAZENADO);
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
        ordem.setSequenciaNavio(5);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return ordem;
    }

    private WorkQueuePatio fila() {
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setId(20L);
        fila.setEquipamentoPatioId(30L);
        fila.setEquipamento("RTG-01");
        return fila;
    }

    private EquipamentoPatio equipamento() {
        return new EquipamentoPatio(30L, "RTG-01", TipoEquipamento.RTG, 1, 1,
                StatusEquipamento.OPERACIONAL);
    }

    private EventoVmtWorkInstructionRequest requestGrounding() {
        EventoVmtWorkInstructionRequest request = new EventoVmtWorkInstructionRequest();
        request.setTipoAcaoFisica(TipoAcaoFisicaPatio.GROUNDING);
        request.setCodigoUnidadeLido("CONT-10");
        request.setEquipamentoPatioId(30L);
        request.setEquipamentoIdentificador("RTG-01");
        request.setOrigem("EXCHANGE-AREA-01");
        request.setDestino("BLOCO-A");
        request.setLinhaDestino(1);
        request.setColunaDestino(2);
        request.setCamadaDestino("T1");
        request.setSequenciaOperacional(5);
        return request;
    }
}
