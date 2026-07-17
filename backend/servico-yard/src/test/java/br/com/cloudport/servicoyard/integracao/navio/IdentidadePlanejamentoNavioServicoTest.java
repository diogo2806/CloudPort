package br.com.cloudport.servicoyard.integracao.navio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentidadePlanejamentoNavioServicoTest {
    private IdentidadePlanejamentoNavioServico servico;
    @BeforeEach void configurar() {
        servico = new IdentidadePlanejamentoNavioServico(new ConsultaPlanejamentoNavioPorta() {
            @Override public NavioPlanejamento buscarNavioPorId(Long id) {
                return new NavioPlanejamento(10L, "NAVIO TESTE", "IMO1234567", "PTABC", 4L);
            }
            @Override public NavioPlanejamento buscarNavioPorImo(String imo) { return buscarNavioPorId(10L); }
            @Override public VisitaPlanejamento buscarVisitaPorId(Long id) {
                return new VisitaPlanejamento(20L, 10L, "VISITA-20", "IN-001", "OUT-002", "PREVISTA", 7L);
            }
        });
    }
    @Test void deveResolverNavioEViagemDaVisitaCanonica() {
        ContextoPlanejamentoNavio contexto = servico.resolverVisita(20L, 10L, "imo1234567", "out-002");
        assertEquals(10L, contexto.navio().identificador());
        assertEquals(20L, contexto.visita().identificador());
        assertEquals("OUT-002", contexto.codigoViagem());
    }
    @Test void deveRecusarVisitaDeOutroNavio() {
        assertThrows(IllegalArgumentException.class, () -> servico.resolverVisita(20L, 99L, "IMO1234567", "OUT-002"));
    }
    @Test void deveRecusarPlanoComVersaoCanonicaDesatualizada() {
        assertThrows(IllegalStateException.class, () -> servico.validarFontePersistida(
                20L, 10L, "IMO1234567", "OUT-002", 3L, 7L));
    }
    @Test void deveRecusarViagemForaDaVisita() {
        assertThrows(IllegalArgumentException.class, () -> servico.resolverVisita(
                20L, 10L, "IMO1234567", "VIAGEM-INVALIDA"));
    }
}
