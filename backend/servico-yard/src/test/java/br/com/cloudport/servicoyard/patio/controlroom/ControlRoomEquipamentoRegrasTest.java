package br.com.cloudport.servicoyard.patio.controlroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ControlRoomEquipamentoRegrasTest {

    @Test
    void deveNormalizarComandoSuportado() {
        assertEquals("SINCRONIZAR_TELEMETRIA",
                ControlRoomEquipamentoRegras.normalizarTipoComando(" sincronizar_telemetria "));
    }

    @Test
    void deveRejeitarComandoNaoSuportado() {
        assertThrows(IllegalArgumentException.class,
                () -> ControlRoomEquipamentoRegras.normalizarTipoComando("DESLIGAR_FORCADO"));
    }

    @Test
    void deveIdentificarTelemetriaAtrasada() {
        LocalDateTime agora = LocalDateTime.of(2026, 7, 18, 10, 0);

        assertTrue(ControlRoomEquipamentoRegras.telemetriaAtrasada(agora.minusMinutes(3), agora));
        assertFalse(ControlRoomEquipamentoRegras.telemetriaAtrasada(agora.minusSeconds(30), agora));
        assertTrue(ControlRoomEquipamentoRegras.telemetriaAtrasada(null, agora));
    }

    @Test
    void deveIdentificarHeartbeatConectado() {
        LocalDateTime agora = LocalDateTime.of(2026, 7, 18, 10, 0);

        assertTrue(ControlRoomEquipamentoRegras.dispositivoConectado(agora.minusSeconds(30), agora));
        assertFalse(ControlRoomEquipamentoRegras.dispositivoConectado(agora.minusMinutes(2), agora));
        assertFalse(ControlRoomEquipamentoRegras.dispositivoConectado(null, agora));
    }

    @Test
    void deveNormalizarStatusDeIntegracao() {
        assertEquals("CONECTADO", ControlRoomEquipamentoRegras.normalizarStatusIntegracao(null));
        assertEquals("DEGRADADO", ControlRoomEquipamentoRegras.normalizarStatusIntegracao(" degradado "));
        assertThrows(IllegalArgumentException.class,
                () -> ControlRoomEquipamentoRegras.normalizarStatusIntegracao("INDEFINIDO"));
    }
}
