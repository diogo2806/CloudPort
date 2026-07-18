package br.com.cloudport.servicoyard.patio.controlroom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

public final class ControlRoomEquipamentoRegras {

    public static final Duration LIMITE_TELEMETRIA = Duration.ofMinutes(2);
    public static final Duration LIMITE_HEARTBEAT = Duration.ofSeconds(90);

    private static final Set<String> TIPOS_COMANDO = Set.of(
            "DISPONIBILIZAR",
            "INDISPONIBILIZAR",
            "ENVIAR_MENSAGEM",
            "MOVER_PARA_POSICAO",
            "SINCRONIZAR_TELEMETRIA",
            "RESETAR_POSICAO"
    );

    private static final Set<String> STATUS_CONFIRMACAO = Set.of("EXECUTADO", "FALHOU");
    private static final Set<String> STATUS_INTEGRACAO = Set.of("CONECTADO", "DESCONECTADO", "DEGRADADO", "ERRO");

    private ControlRoomEquipamentoRegras() {
    }

    public static String normalizarTipoComando(String valor) {
        String normalizado = normalizarObrigatorio(valor, "Tipo do comando");
        if (!TIPOS_COMANDO.contains(normalizado)) {
            throw new IllegalArgumentException("Tipo de comando nao suportado: " + valor + ".");
        }
        return normalizado;
    }

    public static String normalizarStatusConfirmacao(String valor) {
        String normalizado = normalizarObrigatorio(valor, "Status da confirmacao");
        if (!STATUS_CONFIRMACAO.contains(normalizado)) {
            throw new IllegalArgumentException("A confirmacao deve informar EXECUTADO ou FALHOU.");
        }
        return normalizado;
    }

    public static String normalizarStatusIntegracao(String valor) {
        String normalizado = valor == null || valor.isBlank() ? "CONECTADO" : valor.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_INTEGRACAO.contains(normalizado)) {
            throw new IllegalArgumentException("Status de integracao invalido: " + valor + ".");
        }
        return normalizado;
    }

    public static boolean telemetriaAtrasada(LocalDateTime recebidaEm, LocalDateTime agora) {
        return recebidaEm == null || recebidaEm.isBefore(agora.minus(LIMITE_TELEMETRIA));
    }

    public static boolean dispositivoConectado(LocalDateTime heartbeatEm, LocalDateTime agora) {
        return heartbeatEm != null && !heartbeatEm.isBefore(agora.minus(LIMITE_HEARTBEAT));
    }

    public static String normalizarIdentificador(String valor) {
        return normalizarObrigatorio(valor, "Identificador");
    }

    private static String normalizarObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
