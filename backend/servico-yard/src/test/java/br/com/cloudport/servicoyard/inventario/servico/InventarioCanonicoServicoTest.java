package br.com.cloudport.servicoyard.inventario.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.inventario.dto.InventarioCanonicoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.ContagemInventarioFisico;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.modelo.VinculoEquipamento;
import br.com.cloudport.servicoyard.inventario.repositorio.ContagemInventarioFisicoRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.PrefixoEquipamentoInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.TipoEquipamentoInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.VinculoEquipamentoRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class InventarioCanonicoServicoTest {

    private final UnidadeInventarioRepositorio unidadeRepositorio = mock(UnidadeInventarioRepositorio.class);
    private final TipoEquipamentoInventarioRepositorio tipoRepositorio = mock(TipoEquipamentoInventarioRepositorio.class);
    private final PrefixoEquipamentoInventarioRepositorio prefixoRepositorio = mock(PrefixoEquipamentoInventarioRepositorio.class);
    private final VinculoEquipamentoRepositorio vinculoRepositorio = mock(VinculoEquipamentoRepositorio.class);
    private final ContagemInventarioFisicoRepositorio contagemRepositorio = mock(ContagemInventarioFisicoRepositorio.class);
    private final ConteinerPatioRepositorio conteinerPatioRepositorio = mock(ConteinerPatioRepositorio.class);

    private InventarioCanonicoServico servico;

    @BeforeEach
    void configurar() {
        servico = new InventarioCanonicoServico(
                unidadeRepositorio,
                tipoRepositorio,
                prefixoRepositorio,
                vinculoRepositorio,
                contagemRepositorio,
                conteinerPatioRepositorio,
                new SanitizadorEntrada());
    }

    @Test
    void deveBloquearLiberacaoQuandoExisteHoldAtivo() {
        UnidadeInventario unidade = criarUnidade(1L, "ABCD1234567",
                TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
        unidade.setEstado(UnidadeInventario.EstadoUnidade.NO_PATIO);
        unidade.getRestricoes().add(new UnidadeInventario.RestricaoRegistro(
                UnidadeInventario.TipoRestricao.HOLD,
                "CUSTOMS",
                "Bloqueio aduaneiro",
                "ALFANDEGA",
                true,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now()));
        when(unidadeRepositorio.findById(1L)).thenReturn(Optional.of(unidade));

        assertThatThrownBy(() -> servico.atualizarEstado(
                1L,
                new InventarioCanonicoDTO.AtualizarEstadoRequest(
                        UnidadeInventario.EstadoUnidade.LIBERADA,
                        "Solicitação do gate",
                        "operador",
                        "TESTE")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hold ativo");

        verify(unidadeRepositorio, never()).save(any());
    }

    @Test
    void deveImpedirTransicaoInvalidaDeCicloDeVida() {
        UnidadeInventario unidade = criarUnidade(2L, "EFGH7654321",
                TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
        unidade.setEstado(UnidadeInventario.EstadoUnidade.PRE_AVISADA);
        when(unidadeRepositorio.findById(2L)).thenReturn(Optional.of(unidade));

        assertThatThrownBy(() -> servico.atualizarEstado(
                2L,
                new InventarioCanonicoDTO.AtualizarEstadoRequest(
                        UnidadeInventario.EstadoUnidade.DESPACHADA,
                        "Atalho indevido",
                        "operador",
                        "TESTE")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não é permitida");
    }

    @Test
    void deveValidarCategoriaAoMontarEquipamentoDeTransporte() {
        UnidadeInventario principal = criarUnidade(3L, "CONT123",
                TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
        UnidadeInventario acessorio = criarUnidade(4L, "GENSET01",
                TipoEquipamentoInventario.CategoriaEquipamento.ACESSORIO);
        when(unidadeRepositorio.findById(3L)).thenReturn(Optional.of(principal));
        when(unidadeRepositorio.findById(4L)).thenReturn(Optional.of(acessorio));
        when(vinculoRepositorio.existsByUnidadePrincipalIdAndUnidadeRelacionadaIdAndAtivoTrue(3L, 4L))
                .thenReturn(false);
        when(vinculoRepositorio.existsByUnidadePrincipalIdAndUnidadeRelacionadaIdAndAtivoTrue(4L, 3L))
                .thenReturn(false);

        assertThatThrownBy(() -> servico.montar(new InventarioCanonicoDTO.MontagemRequest(
                3L,
                4L,
                VinculoEquipamento.PapelEquipamento.TRANSPORTE,
                "operador",
                null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("chassi ou carreta");
    }

    @Test
    void deveClassificarDivergenciaDePosicaoNoInventarioFisico() {
        UnidadeInventario unidade = criarUnidade(5L, "IJKL1111111",
                TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
        unidade.setPosicaoAtual("B01-02-3");
        when(unidadeRepositorio.findByIdentificacaoIgnoreCase("IJKL1111111"))
                .thenReturn(Optional.of(unidade));
        when(contagemRepositorio.save(any(ContagemInventarioFisico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventarioCanonicoDTO.ContagemLoteResposta resposta = servico.registrarContagem(
                new InventarioCanonicoDTO.ContagemLoteRequest(
                        "INV-2026-01",
                        List.of(new InventarioCanonicoDTO.ItemContagemRequest(
                                "IJKL1111111",
                                "B01-02-4",
                                true,
                                "Leitura por coletor")),
                        "conferente"));

        assertThat(resposta.totalItens()).isEqualTo(1);
        assertThat(resposta.conferentes()).isZero();
        assertThat(resposta.divergentes()).isEqualTo(1);
        assertThat(resposta.resultados()).singleElement().satisfies(resultado -> {
            assertThat(resultado.status()).isEqualTo(ContagemInventarioFisico.StatusContagem.DIVERGENTE);
            assertThat(resultado.tipoDivergencia()).isEqualTo(ContagemInventarioFisico.TipoDivergencia.POSICAO);
        });
    }

    private UnidadeInventario criarUnidade(Long id,
                                            String identificacao,
                                            TipoEquipamentoInventario.CategoriaEquipamento categoria) {
        TipoEquipamentoInventario tipo = new TipoEquipamentoInventario();
        tipo.setId(id);
        tipo.setCodigo("TIPO-" + id);
        tipo.setDescricao("Tipo " + id);
        tipo.setCategoria(categoria);
        tipo.setAtivo(true);

        UnidadeInventario unidade = new UnidadeInventario();
        unidade.setId(id);
        unidade.setIdentificacao(identificacao);
        unidade.setTipoEquipamento(tipo);
        unidade.setCategoria(categoria);
        unidade.setEstado(UnidadeInventario.EstadoUnidade.ATIVA);
        unidade.setCondicao(UnidadeInventario.CondicaoEquipamento.OPERACIONAL);
        unidade.setStatusManutencao(UnidadeInventario.StatusManutencao.NAO_REQUERIDA);
        return unidade;
    }
}
