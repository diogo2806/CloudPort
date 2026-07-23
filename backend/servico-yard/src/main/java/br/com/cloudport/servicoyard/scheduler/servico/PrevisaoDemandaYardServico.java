package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.EventoVmtWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import br.com.cloudport.servicoyard.scheduler.dto.PrevisaoDemandaYardDto;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PrevisaoDemandaYardServico {

    private static final String VERSAO_MODELO = "yard-demand-baseline-v2";
    private static final int HORAS_JANELA_BASE = 7 * 24;
    private static final int AMOSTRA_MINIMA = 5;
    private static final List<TipoEventoVmt> EVENTOS_DEMANDA = List.of(
            TipoEventoVmt.CONCLUSAO,
            TipoEventoVmt.REHANDLE_CONCLUSAO);

    private final EventoVmtWorkInstructionRepositorio eventoRepositorio;
    private final boolean ajusteTendenciaAtivo;
    private final Clock clock;

    public PrevisaoDemandaYardServico(
            EventoVmtWorkInstructionRepositorio eventoRepositorio,
            @Value("${cloudport.yard.ml.enabled:false}") boolean ajusteTendenciaAtivo) {
        this(eventoRepositorio, ajusteTendenciaAtivo, Clock.systemDefaultZone());
    }

    PrevisaoDemandaYardServico(
            EventoVmtWorkInstructionRepositorio eventoRepositorio,
            boolean ajusteTendenciaAtivo,
            Clock clock) {
        this.eventoRepositorio = eventoRepositorio;
        this.ajusteTendenciaAtivo = ajusteTendenciaAtivo;
        this.clock = clock;
    }

    public PrevisaoDemandaYardDto prever(int horizonteHoras) {
        int horizonte = Math.max(1, Math.min(24, horizonteHoras));
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime inicioRecente = agora.minusDays(7);
        LocalDateTime inicioAnterior = inicioRecente.minusDays(7);
        LocalDateTime inicioAmostra = agora.minusDays(28);

        long amostraTotal = contar(inicioAmostra, agora);
        long movimentosRecentes = contar(inicioRecente, agora);
        long movimentosAnteriores = contar(inicioAnterior, inicioRecente);
        int baseline = projetarParaHorizonte(movimentosRecentes, horizonte);

        if (!ajusteTendenciaAtivo || amostraTotal < AMOSTRA_MINIMA) {
            String motivo = amostraTotal < AMOSTRA_MINIMA
                    ? "Amostra temporal insuficiente: foram encontrados " + amostraTotal
                            + " eventos físicos concluídos nos últimos 28 dias."
                    : "O ajuste de tendência está desativado por configuração operacional.";
            return respostaDeterministica(horizonte, baseline, motivo, agora);
        }

        double fatorTendencia = calcularFatorTendencia(movimentosRecentes, movimentosAnteriores);
        int demandaPrevista = saturar(Math.ceil(baseline * fatorTendencia));

        return new PrevisaoDemandaYardDto(
                "BASELINE_TEMPORAL_VMT",
                VERSAO_MODELO,
                true,
                false,
                horizonte,
                demandaPrevista,
                horizonte * 60,
                0.0,
                baseline,
                demandaPrevista - baseline,
                "Estimativa da quantidade de movimentos físicos de pátio concluídos no horizonte solicitado. "
                        + "A origem são eventos VMT CONCLUSAO e REHANDLE_CONCLUSAO dos últimos 28 dias; "
                        + "a taxa dos últimos sete dias é ajustada por uma tendência limitada entre 75% e 125%. "
                        + "O campo de confiança permanece zerado porque não existe calibração estatística validada.",
                validacoesObrigatorias(),
                agora);
    }

    private long contar(LocalDateTime inicio, LocalDateTime fim) {
        return eventoRepositorio.countByTipoEventoInAndOcorridoEmGreaterThanEqualAndOcorridoEmLessThan(
                EVENTOS_DEMANDA,
                inicio,
                fim);
    }

    private int projetarParaHorizonte(long movimentosRecentes, int horizonte) {
        if (movimentosRecentes <= 0) return 0;
        return saturar(Math.ceil(movimentosRecentes * horizonte / (double) HORAS_JANELA_BASE));
    }

    private double calcularFatorTendencia(long movimentosRecentes, long movimentosAnteriores) {
        if (movimentosRecentes == 0) return 0.75;
        if (movimentosAnteriores == 0) return 1.25;
        return Math.max(0.75, Math.min(1.25, movimentosRecentes / (double) movimentosAnteriores));
    }

    private int saturar(double valor) {
        if (valor <= 0) return 0;
        return valor >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) valor;
    }

    private PrevisaoDemandaYardDto respostaDeterministica(
            int horizonte,
            int baseline,
            String motivo,
            LocalDateTime geradoEm) {
        return new PrevisaoDemandaYardDto(
                "BASELINE_TEMPORAL_VMT",
                VERSAO_MODELO,
                ajusteTendenciaAtivo,
                true,
                horizonte,
                baseline,
                horizonte * 60,
                0.0,
                baseline,
                0,
                motivo + " Foi usada somente a taxa determinística dos eventos VMT concluídos nos últimos sete dias. "
                        + "Planos de posição cancelados, expirados ou acumulados não participam do cálculo.",
                validacoesObrigatorias(),
                geradoEm);
    }

    private List<String> validacoesObrigatorias() {
        return List.of(
                "origem da demanda: eventos VMT físicos concluídos dentro de janelas temporais reais",
                "capacidade e ocupação da posição",
                "restrições operacionais e de segurança",
                "compatibilidade da unidade com bloco e equipamento",
                "reservas, bloqueios e validade do plano");
    }
}
