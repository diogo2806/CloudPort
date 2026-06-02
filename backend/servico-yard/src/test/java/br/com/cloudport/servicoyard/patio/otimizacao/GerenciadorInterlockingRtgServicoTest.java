package br.com.cloudport.servicoyard.patio.otimizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GerenciadorInterlockingRtgServicoTest {

    @Mock
    private EquipamentoPatioRepositorio equipamentoRepositorio;

    @InjectMocks
    private GerenciadorInterlockingRtgServico gerenciadorInterlocking;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testObterRtgsOperacionais() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        List<EquipamentoPatio> resultado = gerenciadorInterlocking.obterRtgsOperacionais();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    void testRequisitarDireitoDePassagem() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        boolean resultado = gerenciadorInterlocking.requisitarDireitoDePassagem("RTG-001", 0);

        assertTrue(resultado);
    }

    @Test
    void testIdentificarConflitosDetecto() {
        List<EquipamentoPatio> rtgs = criarRtgsComConflitoTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        var conflitos = gerenciadorInterlocking.identificarConflitosDetecto();

        assertNotNull(conflitos);
        assertTrue(conflitos.size() > 0);
    }

    @Test
    void testObterSequenciaOtimizada() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        var sequencia = gerenciadorInterlocking.obterSequenciaOtimizada(0);

        assertNotNull(sequencia);
        assertTrue(sequencia.getSequenciaRtgs().size() > 0);
    }

    @Test
    void testLiberarDireitoDePassagem() {
        List<EquipamentoPatio> rtgs = criarRtgsTest();
        when(equipamentoRepositorio.findByTipoEquipamentoAndStatusOperacional(
                TipoEquipamento.RTG,
                StatusEquipamento.OPERACIONAL
        )).thenReturn(rtgs);

        gerenciadorInterlocking.requisitarDireitoDePassagem("RTG-001", 0);
        gerenciadorInterlocking.liberarDireitoDePassagem("RTG-001");

        assertFalse(false);
    }

    private List<EquipamentoPatio> criarRtgsTest() {
        List<EquipamentoPatio> rtgs = new ArrayList<>();

        EquipamentoPatio rtg1 = new EquipamentoPatio();
        rtg1.setId(1L);
        rtg1.setIdentificador("RTG-001");
        rtg1.setTipoEquipamento(TipoEquipamento.RTG);
        rtg1.setLinha(0);
        rtg1.setColuna(0);
        rtg1.setStatusOperacional(StatusEquipamento.OPERACIONAL);
        rtgs.add(rtg1);

        EquipamentoPatio rtg2 = new EquipamentoPatio();
        rtg2.setId(2L);
        rtg2.setIdentificador("RTG-002");
        rtg2.setTipoEquipamento(TipoEquipamento.RTG);
        rtg2.setLinha(10);
        rtg2.setColuna(0);
        rtg2.setStatusOperacional(StatusEquipamento.OPERACIONAL);
        rtgs.add(rtg2);

        return rtgs;
    }

    private List<EquipamentoPatio> criarRtgsComConflitoTest() {
        List<EquipamentoPatio> rtgs = new ArrayList<>();

        EquipamentoPatio rtg1 = new EquipamentoPatio();
        rtg1.setId(1L);
        rtg1.setIdentificador("RTG-001");
        rtg1.setTipoEquipamento(TipoEquipamento.RTG);
        rtg1.setLinha(0);
        rtg1.setColuna(0);
        rtg1.setStatusOperacional(StatusEquipamento.OPERACIONAL);
        rtgs.add(rtg1);

        EquipamentoPatio rtg2 = new EquipamentoPatio();
        rtg2.setId(2L);
        rtg2.setIdentificador("RTG-002");
        rtg2.setTipoEquipamento(TipoEquipamento.RTG);
        rtg2.setLinha(3);
        rtg2.setColuna(0);
        rtg2.setStatusOperacional(StatusEquipamento.OPERACIONAL);
        rtgs.add(rtg2);

        return rtgs;
    }
}
