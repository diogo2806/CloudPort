package br.com.cloudport.servicorail.ferrovia.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.ferrovia.evento.PublicadorEventoMovimentacaoTrem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdemMovimentacaoServicoTest {

    @Mock
    private OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    @Mock
    private PublicadorEventoMovimentacaoTrem publicadorEventoMovimentacaoTrem;

    private OrdemMovimentacaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new OrdemMovimentacaoServico(ordemMovimentacaoRepositorio,
                visitaTremRepositorio,
                publicadorEventoMovimentacaoTrem);
    }

    @Test
    void deveColocarVisitaEmProcessamentoAoIniciarOrdem() {
        VisitaTrem visita = visitaComUmaDescarga();
        OrdemMovimentacao ordem = new OrdemMovimentacao(visita,
                "MSCU0000001",
                TipoMovimentacaoOrdem.DESCARGA_TREM,
                StatusOrdemMovimentacao.PENDENTE);
        prepararRepositorios(visita, ordem);

        servico.atualizarStatus(19L, 8L, StatusOrdemMovimentacao.EM_EXECUCAO);

        assertEquals(StatusVisitaTrem.PROCESSANDO, visita.getStatusVisita());
        assertEquals(StatusOperacaoConteinerVisita.PENDENTE,
                visita.getListaDescarga().get(0).getStatusOperacao());
    }

    @Test
    void deveConcluirManifestoEVisitaAoConcluirUltimaOrdem() {
        VisitaTrem visita = visitaComUmaDescarga();
        visita.setStatusVisita(StatusVisitaTrem.PROCESSANDO);
        OrdemMovimentacao ordem = new OrdemMovimentacao(visita,
                "MSCU0000001",
                TipoMovimentacaoOrdem.DESCARGA_TREM,
                StatusOrdemMovimentacao.EM_EXECUCAO);
        prepararRepositorios(visita, ordem);

        servico.atualizarStatus(19L, 8L, StatusOrdemMovimentacao.CONCLUIDA);

        assertEquals(StatusOperacaoConteinerVisita.CONCLUIDO,
                visita.getListaDescarga().get(0).getStatusOperacao());
        assertEquals(StatusVisitaTrem.CONCLUIDO, visita.getStatusVisita());
    }

    private void prepararRepositorios(VisitaTrem visita, OrdemMovimentacao ordem) {
        when(ordemMovimentacaoRepositorio.findByIdAndVisitaTremId(8L, 19L)).thenReturn(Optional.of(ordem));
        when(ordemMovimentacaoRepositorio.save(any(OrdemMovimentacao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(visitaTremRepositorio.buscarPorIdComListas(19L)).thenReturn(Optional.of(visita));
        when(visitaTremRepositorio.save(any(VisitaTrem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private VisitaTrem visitaComUmaDescarga() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        visita.setStatusVisita(StatusVisitaTrem.CHEGOU);
        visita.definirListaDescarga(List.of(new OperacaoConteinerVisita(
                "MSCU0000001",
                StatusOperacaoConteinerVisita.PENDENTE,
                "VAG-001")));
        return visita;
    }
}
