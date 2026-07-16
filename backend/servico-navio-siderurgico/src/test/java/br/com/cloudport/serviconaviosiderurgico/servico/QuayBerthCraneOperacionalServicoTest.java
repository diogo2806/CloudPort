package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardCliente.WorkQueueValidacaoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dto.AlocacaoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoGuindasteVisitaRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuayBerthCraneOperacionalServicoTest {

    @Mock
    private VisitaNavioServico visitaNavioServico;
    @Mock
    private ItemOperacaoNavioRepositorio itemRepositorio;
    @Mock
    private PlanoGuindasteVisitaRepositorio planoRepositorio;
    @Mock
    private ConsultaWorkQueueYardCliente consultaWorkQueueYardCliente;

    private QuayBerthCraneOperacionalServico servico;

    @BeforeEach
    void configurar() {
        servico = new QuayBerthCraneOperacionalServico(
                visitaNavioServico,
                itemRepositorio,
                planoRepositorio,
                consultaWorkQueueYardCliente);
    }

    @Test
    void deveRejeitarPlanoAntesDeSubstituirPersistenciaQuandoFilaNaoPossuiCobertura() {
        LocalDateTime inicio = LocalDateTime.now().plusHours(1);
        ComandoPlanoGuindasteDTO comando = new ComandoPlanoGuindasteDTO(
                "B01",
                StatusPlanoGuindaste.PUBLICADO,
                List.of(new AlocacaoGuindasteDTO(
                        null,
                        "QC-01",
                        "STS-01",
                        1,
                        101L,
                        1,
                        10,
                        new BigDecimal("12.00"),
                        inicio,
                        inicio.plusHours(2),
                        null)),
                "planejador",
                "Plano operacional");
        WorkQueueValidacaoYardDTO fila = new WorkQueueValidacaoYardDTO();
        fila.setId(101L);
        fila.setVisitaNavioId(1L);
        fila.setBerco("B01");
        fila.setPorao(1);
        fila.setStatus("ATIVA");
        fila.setEquipamentoPatioId(20L);
        fila.setEquipamentoIdentificador("RTG-01");
        fila.setEquipamentoStatus("OPERACIONAL");
        fila.setRecursoCaisId(40L);
        fila.setTotalOrdensDispatchaveis(1);
        fila.setCoberturaValida(false);
        when(consultaWorkQueueYardCliente.listarParaValidacaoPlano(1L)).thenReturn(List.of(fila));

        assertThrows(IllegalArgumentException.class, () -> servico.salvarPlano(1L, comando));

        verify(planoRepositorio, never()).deleteByVisitaNavioId(1L);
        verify(planoRepositorio, never()).saveAll(anyList());
    }
}
