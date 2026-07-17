package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import br.com.cloudport.servicoyard.patio.servico.ValidadorYardPlacementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrdemTrabalhoPatioServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    @Mock
    private ConteinerPatioRepositorio conteinerRepositorio;

    @Mock
    private PosicaoPatioRepositorio posicaoPatioRepositorio;

    @Mock
    private MapaPatioServico mapaPatioServico;

    @Mock
    private OtimizadorRotasPatioServico otimizadorRotas;

    @Mock
    private ValidadorYardPlacementService validadorYardPlacement;

    private OrdemTrabalhoPatioServico servico;
    private ConteinerPatio conteiner;
    private PosicaoPatio destino;

    @BeforeEach
    void setUp() {
        servico = new OrdemTrabalhoPatioServico(
                ordemRepositorio,
                conteinerRepositorio,
                posicaoPatioRepositorio,
                mapaPatioServico,
                otimizadorRotas,
                validadorYardPlacement);
        conteiner = new ConteinerPatio();
        conteiner.setId(1L);
        conteiner.setCodigo("CONT001");
        conteiner.setTipoCarga(TipoCargaConteiner.SECO);
        conteiner.setStatus(StatusConteiner.ARMAZENADO);
        conteiner.setDestino("BERCO_A");
        conteiner.setPosicao(new PosicaoPatio(10L, 1, 1, "CAMADA_1"));
        conteiner.setAtualizadoEm(LocalDateTime.now());
        destino = new PosicaoPatio(20L, 2, 2, "CAMADA_1");
    }

    @Test
    void deveReservarPosicaoECriarOrdemNaMesmaOperacaoIdempotente() {
        OrdemTrabalhoPatioRequisicaoDto requisicao = requisicaoRemanejamento();
        when(ordemRepositorio.findByChaveIdempotencia("RESHUFFLING:CONT001:POSICAO:10"))
                .thenReturn(Optional.empty());
        when(posicaoPatioRepositorio.findByLinhaAndColunaAndCamadaOperacional(2, 2, "CAMADA_1"))
                .thenReturn(Optional.of(destino));
        when(conteinerRepositorio.findByCodigoIgnoreCase("CONT001")).thenReturn(Optional.of(conteiner));
        when(conteinerRepositorio.findAll()).thenReturn(List.of(conteiner));
        when(conteinerRepositorio.findByPosicaoLinhaAndPosicaoColuna(2, 2)).thenReturn(List.of());
        when(ordemRepositorio.existsByLinhaDestinoAndColunaDestinoAndCamadaDestinoIgnoreCaseAndStatusOrdemIn(
                2, 2, "CAMADA_1", statusAtivos())).thenReturn(false);
        when(ordemRepositorio.existsByCodigoConteinerIgnoreCaseAndStatusOrdemIn("CONT001", statusAtivos()))
                .thenReturn(false);
        when(ordemRepositorio.save(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        servico.registrarOuReutilizarRemanejamento(requisicao);

        assertThat(destino.getReservaChave()).isEqualTo("RESHUFFLING:CONT001:POSICAO:10");
        assertThat(destino.getReservaCodigoConteiner()).isEqualTo("CONT001");
        assertThat(destino.getReservaExpiraEm()).isAfter(LocalDateTime.now());
        verify(posicaoPatioRepositorio).save(destino);
        ArgumentCaptor<OrdemTrabalhoPatio> ordemCaptor = ArgumentCaptor.forClass(OrdemTrabalhoPatio.class);
        verify(ordemRepositorio).save(ordemCaptor.capture());
        assertThat(ordemCaptor.getValue().getChaveIdempotencia())
                .isEqualTo("RESHUFFLING:CONT001:POSICAO:10");
    }

    @Test
    void deveReutilizarOrdemQuandoAChaveJaExistir() {
        OrdemTrabalhoPatio existente = ordemExistente();
        when(ordemRepositorio.findByChaveIdempotencia("RESHUFFLING:CONT001:POSICAO:10"))
                .thenReturn(Optional.of(existente));

        servico.registrarOuReutilizarRemanejamento(requisicaoRemanejamento());

        verify(posicaoPatioRepositorio, never())
                .findByLinhaAndColunaAndCamadaOperacional(any(), any(), any());
        verify(ordemRepositorio, never()).save(any(OrdemTrabalhoPatio.class));
    }

    @Test
    void deveRejeitarRemanejamentoParaPosicaoInexistente() {
        when(ordemRepositorio.findByChaveIdempotencia("RESHUFFLING:CONT001:POSICAO:10"))
                .thenReturn(Optional.empty());
        when(posicaoPatioRepositorio.findByLinhaAndColunaAndCamadaOperacional(2, 2, "CAMADA_1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.registrarOuReutilizarRemanejamento(requisicaoRemanejamento()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private OrdemTrabalhoPatioRequisicaoDto requisicaoRemanejamento() {
        OrdemTrabalhoPatioRequisicaoDto dto = new OrdemTrabalhoPatioRequisicaoDto();
        dto.setCodigoConteiner("CONT001");
        dto.setTipoCarga("SECO");
        dto.setDestino("BERCO_A");
        dto.setLinhaDestino(2);
        dto.setColunaDestino(2);
        dto.setCamadaDestino("CAMADA_1");
        dto.setTipoMovimento(TipoMovimentoPatio.REMANEJAMENTO);
        dto.setStatusConteinerDestino(StatusConteiner.ARMAZENADO);
        dto.setChaveIdempotencia("RESHUFFLING:CONT001:POSICAO:10");
        return dto;
    }

    private OrdemTrabalhoPatio ordemExistente() {
        LocalDateTime agora = LocalDateTime.now();
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                conteiner,
                "CONT001",
                "SECO",
                "BERCO_A",
                2,
                2,
                "CAMADA_1",
                TipoMovimentoPatio.REMANEJAMENTO,
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusConteiner.ARMAZENADO,
                agora,
                agora);
        ordem.setId(50L);
        ordem.setChaveIdempotencia("RESHUFFLING:CONT001:POSICAO:10");
        return ordem;
    }

    private List<StatusOrdemTrabalhoPatio> statusAtivos() {
        return List.of(
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.SUSPENSA,
                StatusOrdemTrabalhoPatio.BLOQUEADA);
    }
}
