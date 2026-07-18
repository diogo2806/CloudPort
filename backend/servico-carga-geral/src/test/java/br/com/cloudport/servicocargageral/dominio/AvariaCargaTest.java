package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAvariaCarga;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvariaCargaTest {

    @Test
    void deveExecutarCicloCompletoDeInspecaoEReparo() {
        AvariaCarga avaria = novaAvaria();

        avaria.adicionarEvidencia("foto", "https://evidencias.local/avaria-1.jpg", "abc123", "inspetor");
        avaria.iniciarInspecao("inspetor", "Dano confirmado.");
        avaria.iniciarReparo("equipe-reparo", "Substituir embalagem.");
        avaria.concluirReparo("equipe-reparo", "Embalagem substituída.");

        assertEquals(StatusAvariaCarga.REPARADA, avaria.getStatus());
        assertEquals("inspetor", avaria.getInspecionadoPor());
        assertEquals("equipe-reparo", avaria.getReparadoPor());
        assertEquals("Embalagem substituída.", avaria.getObservacoes());
        assertEquals(1, avaria.getEvidencias().size());
        assertEquals("FOTO", avaria.getEvidencias().get(0).getTipo());
    }

    @Test
    void deveRejeitarConclusaoDeReparoSemEtapasAnteriores() {
        AvariaCarga avaria = novaAvaria();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> avaria.concluirReparo("operador", "Tentativa inválida."));

        assertEquals("Avaria deve estar em reparo para ser concluída.", exception.getMessage());
        assertEquals(StatusAvariaCarga.BLOQUEADA, avaria.getStatus());
        assertNull(avaria.getReparadoPor());
    }

    @Test
    void deveImpedirEvidenciaDepoisDaBaixa() {
        AvariaCarga avaria = novaAvaria();
        avaria.baixar("supervisor", "Carga sem condição de uso.");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> avaria.adicionarEvidencia("LAUDO", "DOC-123", null, "supervisor"));

        assertEquals("Avaria já está encerrada.", exception.getMessage());
        assertEquals(StatusAvariaCarga.BAIXADA, avaria.getStatus());
    }

    private AvariaCarga novaAvaria() {
        AvariaCarga avaria = new AvariaCarga();
        avaria.setCommandId(UUID.randomUUID());
        avaria.setLoteId(UUID.randomUUID());
        avaria.setCodigo("EMBALAGEM_RASGADA");
        avaria.setDescricao("Embalagens rasgadas durante a descarga.");
        avaria.setQuantidadeAfetada(decimal("10"));
        avaria.setVolumeAfetadoM3(decimal("2.5"));
        avaria.setPesoAfetadoKg(decimal("800"));
        avaria.setResponsavel("operador");
        return avaria;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
