package br.com.cloudport.servicoyard.dispatch.servico;

import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.dispatch.dto.CadastroInstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.DespachoInstrucaoDTO;
import br.com.cloudport.servicoyard.dispatch.dto.InstrucaoMovimentacaoDTO;
import br.com.cloudport.servicoyard.dispatch.modelo.InstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.modelo.StatusInstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.modelo.TipoMoveVmt;
import br.com.cloudport.servicoyard.dispatch.repositorio.InstrucaoMovimentacaoRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstrucaoMovimentacaoServicoTest {

    private InstrucaoMovimentacaoRepositorio instrucaoRepositorio;
    private EquipamentoPatioRepositorio equipamentoRepositorio;
    private InstrucaoMovimentacaoServico servico;

    @BeforeEach
    void preparar() {
        instrucaoRepositorio = mock(InstrucaoMovimentacaoRepositorio.class);
        equipamentoRepositorio = mock(EquipamentoPatioRepositorio.class);
        SanitizadorEntrada sanitizadorEntrada = mock(SanitizadorEntrada.class);
        when(sanitizadorEntrada.limparTexto(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(instrucaoRepositorio.save(any(InstrucaoMovimentacao.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        servico = new InstrucaoMovimentacaoServico(instrucaoRepositorio, equipamentoRepositorio, sanitizadorEntrada);
    }

    private CadastroInstrucaoMovimentacaoDTO cadastro() {
        CadastroInstrucaoMovimentacaoDTO dto = new CadastroInstrucaoMovimentacaoDTO();
        dto.setCodigoConteiner("msku1234567");
        dto.setTipoMove(TipoMoveVmt.DESCARGA_NAVIO);
        dto.setPosicaoDestino("A01-02-03");
        dto.setPrioridadeFetch(true);
        return dto;
    }

    private EquipamentoPatio equipamento() {
        return new EquipamentoPatio(7L, "RTG-07", TipoEquipamento.RTG, 1, 1, StatusEquipamento.OPERACIONAL);
    }

    @Test
    void planejarCriaInstrucaoPlanejadaComCodigoNormalizado() {
        InstrucaoMovimentacaoDTO resultado = servico.planejar(cadastro());

        assertThat(resultado.getStatus()).isEqualTo(StatusInstrucaoMovimentacao.PLANEJADA);
        assertThat(resultado.getCodigoConteiner()).isEqualTo("MSKU1234567");
        assertThat(resultado.getTipoMove()).isEqualTo(TipoMoveVmt.DESCARGA_NAVIO);
        assertThat(resultado.isPrioridadeFetch()).isTrue();
    }

    @Test
    void despacharAtribuiEquipamentoEMarcaDespachada() {
        InstrucaoMovimentacao instrucao = new InstrucaoMovimentacao();
        instrucao.setStatus(StatusInstrucaoMovimentacao.PLANEJADA);
        when(instrucaoRepositorio.findById(1L)).thenReturn(Optional.of(instrucao));
        when(equipamentoRepositorio.findById(7L)).thenReturn(Optional.of(equipamento()));

        DespachoInstrucaoDTO dto = new DespachoInstrucaoDTO();
        dto.setEquipamentoId(7L);

        InstrucaoMovimentacaoDTO resultado = servico.despachar(1L, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusInstrucaoMovimentacao.DESPACHADA);
        assertThat(resultado.getEquipamentoIdentificador()).isEqualTo("RTG-07");
    }

    @Test
    void despacharFalhaQuandoEquipamentoInexistente() {
        InstrucaoMovimentacao instrucao = new InstrucaoMovimentacao();
        instrucao.setStatus(StatusInstrucaoMovimentacao.PLANEJADA);
        when(instrucaoRepositorio.findById(1L)).thenReturn(Optional.of(instrucao));
        when(equipamentoRepositorio.findById(99L)).thenReturn(Optional.empty());

        DespachoInstrucaoDTO dto = new DespachoInstrucaoDTO();
        dto.setEquipamentoId(99L);

        assertThatThrownBy(() -> servico.despachar(1L, dto)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void iniciarExigeStatusDespachada() {
        InstrucaoMovimentacao instrucao = new InstrucaoMovimentacao();
        instrucao.setStatus(StatusInstrucaoMovimentacao.PLANEJADA);
        when(instrucaoRepositorio.findById(1L)).thenReturn(Optional.of(instrucao));

        assertThatThrownBy(() -> servico.iniciar(1L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void concluirMovimentaParaConcluida() {
        InstrucaoMovimentacao instrucao = new InstrucaoMovimentacao();
        instrucao.setStatus(StatusInstrucaoMovimentacao.EM_EXECUCAO);
        when(instrucaoRepositorio.findById(1L)).thenReturn(Optional.of(instrucao));

        InstrucaoMovimentacaoDTO resultado = servico.concluir(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusInstrucaoMovimentacao.CONCLUIDA);
        assertThat(resultado.getConcluidoEm()).isNotNull();
    }
}
