package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ItemCargoSiderurgico;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoCargaSiderurgica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("RegrasCargaSiderurgicaServico - Regras por tipo de produto")
class RegrasCargaSiderurgicaServicoTest {

    private RegrasCargaSiderurgicaServico servico;

    @BeforeEach
    void setup() {
        servico = new RegrasCargaSiderurgicaServico();
    }

    @Test
    @DisplayName("Bobina exige berço e tem max 3 camadas")
    void bobinaExigeBerceiroE3Camadas() {
        assertTrue(servico.requerBerceiro(TipoCargaSiderurgica.BOBINA_ACO));
        assertEquals(3, servico.maxCamadasPorTipo(TipoCargaSiderurgica.BOBINA_ACO));
    }

    @Test
    @DisplayName("Slab exige reforço de piso e tem max 2 camadas")
    void slabExigeReforcoPisoE2Camadas() {
        assertTrue(servico.requerReforcoPiso(TipoCargaSiderurgica.SLAB));
        assertEquals(2, servico.maxCamadasPorTipo(TipoCargaSiderurgica.SLAB));
        assertFalse(servico.requerBerceiro(TipoCargaSiderurgica.SLAB));
    }

    @Test
    @DisplayName("Vergalhão e tarugo permitem empilhamento misto")
    void vergalhaoTarugoPermitemMisto() {
        assertTrue(servico.permiteEmpilhamentoMisto(TipoCargaSiderurgica.VERGALHAO));
        assertTrue(servico.permiteEmpilhamentoMisto(TipoCargaSiderurgica.TARUGO));
        assertFalse(servico.permiteEmpilhamentoMisto(TipoCargaSiderurgica.BOBINA_ACO));
    }

    @Test
    @DisplayName("Item sem porto de descarga gera violação PERIGO")
    void itemSemPortoGeraViolacao() {
        ItemCargoSiderurgico item = criarItem("IT-001", TipoCargaSiderurgica.BOBINA_ACO, 10000.0, null);

        List<ViolacaoEstivaDto> violacoes = servico.validarItem(item);

        assertTrue(violacoes.stream()
                .anyMatch(v -> "PORTO_DESCARGA_AUSENTE".equals(v.getTipo()) && "PERIGO".equals(v.getSeveridade())),
                "Deve detectar porto de descarga ausente");
    }

    @Test
    @DisplayName("Item válido não gera violações de PERIGO")
    void itemValidoSemViolacoes() {
        ItemCargoSiderurgico item = criarItem("IT-002", TipoCargaSiderurgica.CHAPA_ACO, 8000.0, "BRSSV");

        List<ViolacaoEstivaDto> violacoes = servico.validarItem(item);

        assertTrue(violacoes.stream().noneMatch(v -> "PERIGO".equals(v.getSeveridade())));
    }

    @Test
    @DisplayName("Slab com mais de 30t gera AVISO")
    void slabSobrepesadoGeraAviso() {
        ItemCargoSiderurgico item = criarItem("SL-001", TipoCargaSiderurgica.SLAB, 35000.0, "CNSHA");

        List<ViolacaoEstivaDto> violacoes = servico.validarItem(item);

        assertTrue(violacoes.stream()
                .anyMatch(v -> "SLAB_PESO_EXCEDIDO".equals(v.getTipo()) && "AVISO".equals(v.getSeveridade())));
    }

    @Test
    @DisplayName("aplicarPadroesParaTipo define campos corretos para bobina")
    void aplicarPadroesDefineCamposBobina() {
        ItemCargoSiderurgico item = new ItemCargoSiderurgico();
        item.setTipoCarga(TipoCargaSiderurgica.BOBINA_ACO);

        servico.aplicarPadroesParaTipo(item);

        assertTrue(item.getRequerBerceiro());
        assertFalse(item.getRequerReforcoPiso());
        assertEquals(3, item.getMaxCamadasEmpilhamento());
    }

    private ItemCargoSiderurgico criarItem(String codigo, TipoCargaSiderurgica tipo,
                                            double pesoKg, String portoDescarga) {
        ItemCargoSiderurgico item = new ItemCargoSiderurgico();
        item.setCodigo(codigo);
        item.setTipoCarga(tipo);
        item.setPesoKg(pesoKg);
        item.setPortoDescarga(portoDescarga);
        if (tipo == TipoCargaSiderurgica.BOBINA_ACO) {
            item.setDiametroExternoMm(1500.0);
        }
        return item;
    }
}
