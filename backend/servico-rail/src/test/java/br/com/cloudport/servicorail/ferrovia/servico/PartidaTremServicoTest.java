package br.com.cloudport.servicorail.ferrovia.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PartidaTremServicoTest {

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    private PartidaTremServico servico;

    @BeforeEach
    void configurar() {
        servico = new PartidaTremServico(visitaTremRepositorio);
    }

    @Test
    void deveRegistrarPartidaQuandoManifestoEstaConcluido() {
        VisitaTrem visita = criarVisita(StatusVisitaTrem.CONCLUIDO, StatusOperacaoConteinerVisita.CONCLUIDO);
        when(visitaTremRepositorio.buscarPorIdComListas(19L)).thenReturn(Optional.of(visita));
        when(visitaTremRepositorio.save(any(VisitaTrem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servico.registrarPartida(19L);

        assertEquals(StatusVisitaTrem.PARTIU, visita.getStatusVisita());
    }

    @Test
    void deveRecusarPartidaAntesDaConclusaoDaVisita() {
        VisitaTrem visita = criarVisita(StatusVisitaTrem.PROCESSANDO, StatusOperacaoConteinerVisita.PENDENTE);
        when(visitaTremRepositorio.buscarPorIdComListas(19L)).thenReturn(Optional.of(visita));

        assertThrows(ResponseStatusException.class, () -> servico.registrarPartida(19L));
        assertEquals(StatusVisitaTrem.PROCESSANDO, visita.getStatusVisita());
    }

    private VisitaTrem criarVisita(StatusVisitaTrem statusVisita,
                                    StatusOperacaoConteinerVisita statusOperacao) {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        visita.setStatusVisita(statusVisita);
        visita.definirListaDescarga(List.of(new OperacaoConteinerVisita(
                "MSCU0000001",
                statusOperacao,
                "VAG-001")));
        return visita;
    }
}
