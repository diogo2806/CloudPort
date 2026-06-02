package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IntegracaoCronogramaNavioServicoTest {

    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;

    @InjectMocks
    private IntegracaoCronogramaNavioServico integracaoCronograma;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalcularPriorizacaoRtgPorNavio() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        LocalDateTime dataPartida = LocalDateTime.now().plusHours(4);
        var priorizacao = integracaoCronograma.calcularPriorizacaoRtgPorNavio(dataPartida);

        assertNotNull(priorizacao);
        assertEquals("CRÍTICA", priorizacao.getNivelUrgencia());
        assertTrue(priorizacao.getPriorizacoes().size() > 0);
    }

    @Test
    void testObterSequenciaOtimizadaPorNavio() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        LocalDateTime dataPartida = LocalDateTime.now().plusHours(2);
        var sequencia = integracaoCronograma.obterSequenciaOtimizadaPorNavio(dataPartida);

        assertNotNull(sequencia);
        assertTrue(sequencia.size() > 0);
    }

    @Test
    void testAnalisarCapacidadeParaNavio() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);
        when(ordemRepositorio.findAll()).thenReturn(new ArrayList<>());

        LocalDateTime dataPartida = LocalDateTime.now().plusHours(8);
        var analise = integracaoCronograma.analisarCapacidadeParaNavio(dataPartida);

        assertNotNull(analise);
        assertTrue(analise.getPercentualCapacidade() >= 0);
    }

    @Test
    void testIdentificarAlertasOperacionais() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);
        when(ordemRepositorio.findAll()).thenReturn(new ArrayList<>());

        LocalDateTime dataPartida = LocalDateTime.now().plusHours(1);
        var alertas = integracaoCronograma.identificarAlertasOperacionais(dataPartida);

        assertNotNull(alertas);
    }

    private List<EquipamentoPatio> criarRtgsTest() {
        List<EquipamentoPatio> rtgs = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            EquipamentoPatio rtg = new EquipamentoPatio();
            rtg.setId((long) i + 1);
            rtg.setIdentificador("RTG-00" + (i + 1));
            rtg.setTipoEquipamento(TipoEquipamento.RTG);
            rtg.setLinha(i * 10);
            rtg.setColuna(i * 10);
            rtg.setStatusOperacional(StatusEquipamento.OPERACIONAL);
            rtgs.add(rtg);
        }

        return rtgs;
    }
}
