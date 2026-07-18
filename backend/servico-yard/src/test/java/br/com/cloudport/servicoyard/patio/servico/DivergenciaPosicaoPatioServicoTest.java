package br.com.cloudport.servicoyard.patio.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.DivergenciaPosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.StatusDivergenciaPosicao;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.repositorio.DivergenciaPosicaoPatioRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DivergenciaPosicaoPatioServicoTest {

    @Mock
    private DivergenciaPosicaoPatioRepositorio divergenciaRepositorio;

    @Mock
    private UnidadeInventarioRepositorio unidadeRepositorio;

    @Mock
    private InstrucaoTrabalhoServico instrucaoServico;

    @InjectMocks
    private DivergenciaPosicaoPatioServico servico;

    @Test
    void devePreservarCondicaoAnteriorAoAbrirERestaurarAoCancelar() {
        UnidadeInventario unidade = criarUnidade(UnidadeInventario.CondicaoEquipamento.AVARIADO);
        when(unidadeRepositorio.findComBloqueioByIdentificacaoIgnoreCase("MSCU1234567"))
                .thenReturn(Optional.of(unidade));
        when(divergenciaRepositorio.findFirstByUnidadeIdAndStatusIn(eq(10L), any()))
                .thenReturn(Optional.empty());
        when(divergenciaRepositorio.save(any(DivergenciaPosicaoPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        DivergenciaPosicaoPatio caso = servico.abrir(
                "MSCU1234567", "1/2/1", "1/3/1", "Leitura por coletor", "operador");

        assertThat(caso.getCondicaoAnterior()).isEqualTo(UnidadeInventario.CondicaoEquipamento.AVARIADO);
        assertThat(unidade.getCondicao()).isEqualTo(UnidadeInventario.CondicaoEquipamento.EM_INSPECAO);

        when(divergenciaRepositorio.findById(1L)).thenReturn(Optional.of(caso));
        DivergenciaPosicaoPatio cancelado = servico.cancelar(1L, "Leitura física descartada");

        assertThat(unidade.getCondicao()).isEqualTo(UnidadeInventario.CondicaoEquipamento.AVARIADO);
        assertThat(cancelado.getStatus()).isEqualTo(StatusDivergenciaPosicao.CANCELADA);
        assertThat(cancelado.isBloqueada()).isFalse();
    }

    @Test
    void deveCancelarInstrucaoCorretivaAntesDeLiberarCaso() {
        UnidadeInventario unidade = criarUnidade(UnidadeInventario.CondicaoEquipamento.EM_INSPECAO);
        DivergenciaPosicaoPatio caso = criarCaso(unidade);
        InstrucaoTrabalho instrucao = org.mockito.Mockito.mock(InstrucaoTrabalho.class);
        when(instrucao.getId()).thenReturn(99L);
        when(instrucao.getStatus()).thenReturn(StatusInstrucao.PENDENTE);
        caso.setInstrucaoCorretiva(instrucao);

        when(divergenciaRepositorio.findById(1L)).thenReturn(Optional.of(caso));
        when(instrucaoServico.obter(99L)).thenReturn(instrucao);
        when(divergenciaRepositorio.save(any(DivergenciaPosicaoPatio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        servico.cancelar(1L, "Posição lógica corrigida por inventário");

        verify(instrucaoServico).cancelar(eq(99L), contains("Cancelamento da divergência de posição"));
        assertThat(unidade.getCondicao()).isEqualTo(UnidadeInventario.CondicaoEquipamento.OPERACIONAL);
        assertThat(caso.getStatus()).isEqualTo(StatusDivergenciaPosicao.CANCELADA);
        assertThat(caso.isBloqueada()).isFalse();
    }

    @Test
    void naoDeveLiberarCasoComInstrucaoCorretivaConcluida() {
        UnidadeInventario unidade = criarUnidade(UnidadeInventario.CondicaoEquipamento.EM_INSPECAO);
        DivergenciaPosicaoPatio caso = criarCaso(unidade);
        InstrucaoTrabalho instrucao = org.mockito.Mockito.mock(InstrucaoTrabalho.class);
        when(instrucao.getId()).thenReturn(99L);
        when(instrucao.getStatus()).thenReturn(StatusInstrucao.CONCLUIDA);
        caso.setInstrucaoCorretiva(instrucao);

        when(divergenciaRepositorio.findById(1L)).thenReturn(Optional.of(caso));
        when(instrucaoServico.obter(99L)).thenReturn(instrucao);

        IllegalStateException excecao = assertThrows(
                IllegalStateException.class,
                () -> servico.cancelar(1L, "Cancelamento indevido"));

        assertThat(excecao.getMessage()).contains("deve ser resolvida");
        assertThat(unidade.getCondicao()).isEqualTo(UnidadeInventario.CondicaoEquipamento.EM_INSPECAO);
        assertThat(caso.getStatus()).isEqualTo(StatusDivergenciaPosicao.CORRECAO_PENDENTE);
        assertThat(caso.isBloqueada()).isTrue();
        verify(instrucaoServico, never()).cancelar(any(), any());
        verify(unidadeRepositorio, never()).save(any());
        verify(divergenciaRepositorio, never()).save(any(DivergenciaPosicaoPatio.class));
    }

    private UnidadeInventario criarUnidade(UnidadeInventario.CondicaoEquipamento condicao) {
        UnidadeInventario unidade = new UnidadeInventario();
        unidade.setId(10L);
        unidade.setIdentificacao("MSCU1234567");
        unidade.setCondicao(condicao);
        return unidade;
    }

    private DivergenciaPosicaoPatio criarCaso(UnidadeInventario unidade) {
        DivergenciaPosicaoPatio caso = new DivergenciaPosicaoPatio();
        caso.setUnidade(unidade);
        caso.setCondicaoAnterior(UnidadeInventario.CondicaoEquipamento.OPERACIONAL);
        caso.setIdentificacaoUnidade(unidade.getIdentificacao());
        caso.setPosicaoEsperada("1/2/1");
        caso.setPosicaoEncontrada("1/3/1");
        caso.setStatus(StatusDivergenciaPosicao.CORRECAO_PENDENTE);
        caso.setBloqueada(true);
        caso.setAbertaPor("operador");
        return caso;
    }
}
