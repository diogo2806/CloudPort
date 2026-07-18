package br.com.cloudport.servicorail.ferrovia.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.dto.ReplanejamentoConteinerRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.ReplanejamentoConteinerFerroviario;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.ReplanejamentoConteinerFerroviarioRepositorio;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReplanejamentoConteinerFerroviarioServicoTest {

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    @Mock
    private OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;

    @Mock
    private ReplanejamentoConteinerFerroviarioRepositorio replanejamentoRepositorio;

    @Mock
    private SanitizadorEntrada sanitizadorEntrada;

    private ReplanejamentoConteinerFerroviarioServico servico;

    @BeforeEach
    void configurar() {
        servico = new ReplanejamentoConteinerFerroviarioServico(
                visitaTremRepositorio,
                ordemMovimentacaoRepositorio,
                replanejamentoRepositorio,
                sanitizadorEntrada);
        when(sanitizadorEntrada.limparTexto(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveMoverConteinerERegistrarAuditoria() {
        VisitaTrem visita = criarVisita();
        ReplanejamentoConteinerRequisicaoDto requisicao = criarRequisicao();
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));
        when(ordemMovimentacaoRepositorio
                .findByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(
                        19L, "MSCU0000001", TipoMovimentacaoOrdem.DESCARGA_TREM))
                .thenReturn(Optional.empty());
        when(visitaTremRepositorio.saveAndFlush(any(VisitaTrem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(replanejamentoRepositorio.save(any(ReplanejamentoConteinerFerroviario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        servico.replanejarConteinerEntreVagoes(19L, requisicao, "operador.teste");

        assertEquals("VAG-002", visita.getListaDescarga().get(0).getIdentificadorVagao());
        verify(replanejamentoRepositorio).save(any(ReplanejamentoConteinerFerroviario.class));
    }

    @Test
    void deveBloquearDestinoSemCapacidade() {
        VisitaTrem visita = criarVisita();
        visita.definirListaCarga(List.of(
                new OperacaoConteinerVisita("MSCU0000002", StatusOperacaoConteinerVisita.PENDENTE, "VAG-002"),
                new OperacaoConteinerVisita("MSCU0000003", StatusOperacaoConteinerVisita.PENDENTE, "VAG-002")));
        ReplanejamentoConteinerRequisicaoDto requisicao = criarRequisicao();
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));

        assertThrows(ResponseStatusException.class,
                () -> servico.replanejarConteinerEntreVagoes(19L, requisicao, "operador.teste"));
    }

    private VisitaTrem criarVisita() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        visita.setStatusVisita(StatusVisitaTrem.PLANEJADO);
        visita.definirListaVagoes(List.of(
                new VagaoVisita(1, "VAG-001", "PLATAFORMA"),
                new VagaoVisita(2, "VAG-002", "PLATAFORMA")));
        visita.definirListaDescarga(List.of(new OperacaoConteinerVisita(
                "MSCU0000001",
                StatusOperacaoConteinerVisita.PENDENTE,
                "VAG-001")));
        return visita;
    }

    private ReplanejamentoConteinerRequisicaoDto criarRequisicao() {
        ReplanejamentoConteinerRequisicaoDto requisicao = new ReplanejamentoConteinerRequisicaoDto();
        requisicao.setCodigoConteiner("MSCU0000001");
        requisicao.setTipoMovimentacao(TipoMovimentacaoOrdem.DESCARGA_TREM);
        requisicao.setVagaoOrigem("VAG-001");
        requisicao.setVagaoDestino("VAG-002");
        requisicao.setVersaoComposicao(0L);
        requisicao.setMotivo("Balanceamento da composição");
        return requisicao;
    }
}
