package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ProgramacaoDocaCargaTest {

    @Test
    void deveReservarIniciarEConcluirLiberandoRecursos() {
        ProgramacaoDocaCarga programacao = new ProgramacaoDocaCarga();
        OffsetDateTime agora = OffsetDateTime.now();

        programacao.reservar(
                novaOperacao(),
                "doca-01",
                "area-a",
                "empilhadeira-07",
                agora.minusMinutes(5),
                agora.plusHours(2),
                "planejador",
                "Janela confirmada");

        assertEquals(StatusProgramacaoDocaCarga.RESERVADA, programacao.getStatus());
        assertEquals("DOCA-01", programacao.getDocaId());
        assertEquals("AREA-A", programacao.getAreaEsperaId());
        assertEquals("EMPILHADEIRA-07", programacao.getRecursoId());
        assertTrue(programacao.ocupaRecursos());

        programacao.iniciar("operador");
        assertEquals(StatusProgramacaoDocaCarga.EM_USO, programacao.getStatus());
        assertTrue(programacao.ocupaRecursos());

        programacao.concluir("operador");
        assertEquals(StatusProgramacaoDocaCarga.CONCLUIDA, programacao.getStatus());
        assertFalse(programacao.ocupaRecursos());
    }

    @Test
    void deveBloquearInicioAntesDaJanela() {
        ProgramacaoDocaCarga programacao = new ProgramacaoDocaCarga();
        OffsetDateTime agora = OffsetDateTime.now();
        programacao.reservar(
                novaOperacao(),
                "DOCA-02",
                "AREA-B",
                "GUINDASTE-02",
                agora.plusHours(1),
                agora.plusHours(3),
                "planejador",
                null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> programacao.iniciar("operador"));

        assertTrue(exception.getMessage().contains("ainda não foi iniciada"));
    }

    @Test
    void deveCancelarReservaAntesDoInicio() {
        ProgramacaoDocaCarga programacao = new ProgramacaoDocaCarga();
        OffsetDateTime agora = OffsetDateTime.now();
        programacao.reservar(
                novaOperacao(),
                "DOCA-03",
                "AREA-C",
                "EQUIPE-03",
                agora.plusMinutes(10),
                agora.plusHours(2),
                "planejador",
                null);

        programacao.cancelar("planejador", "Navio atrasado", false);

        assertEquals(StatusProgramacaoDocaCarga.CANCELADA, programacao.getStatus());
        assertEquals("Navio atrasado", programacao.getMotivoCancelamento());
        assertFalse(programacao.ocupaRecursos());
    }

    @Test
    void deveImpedirLiberacaoIsoladaQuandoProgramacaoEstaEmUso() {
        ProgramacaoDocaCarga programacao = new ProgramacaoDocaCarga();
        OffsetDateTime agora = OffsetDateTime.now();
        programacao.reservar(
                novaOperacao(),
                "DOCA-04",
                "AREA-D",
                "EQUIPE-04",
                agora.minusMinutes(5),
                agora.plusHours(2),
                "planejador",
                null);
        programacao.iniciar("operador");

        assertThrows(
                IllegalStateException.class,
                () -> programacao.cancelar("planejador", "Troca de doca", false));
    }

    private OperacaoStuffUnstuff novaOperacao() {
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        operacao.setTipo(TipoOperacaoStuffUnstuff.STUFF);
        operacao.setConteinerId("CONT-001");
        return operacao;
    }
}
