package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.scheduler.dto.PlanoPosicaoOperacionalRespostaDto;
import br.com.cloudport.servicoyard.scheduler.dto.PrevisaoDemandaYardDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PrevisaoDemandaYardServico {

    private static final String VERSAO_MODELO = "yard-demand-heuristic-v1";
    private final PlanoPosicaoOperacionalServico planoServico;
    private final boolean modeloAtivo;

    public PrevisaoDemandaYardServico(
            PlanoPosicaoOperacionalServico planoServico,
            @Value("${cloudport.yard.ml.enabled:false}") boolean modeloAtivo) {
        this.planoServico = planoServico;
        this.modeloAtivo = modeloAtivo;
    }

    public PrevisaoDemandaYardDto prever(int horizonteHoras) {
        int horizonte = Math.max(1, Math.min(24, horizonteHoras));
        List<PlanoPosicaoOperacionalRespostaDto> planos = planoServico.listar(null, null);
        int baseline = planos.size();

        if (!modeloAtivo || baseline < 5) {
            return respostaFallback(horizonte, baseline, planos.size() < 5
                    ? "Amostra operacional insuficiente para aplicar o modelo."
                    : "Modelo desativado por configuração operacional.");
        }

        int fatorHorizonte = Math.max(1, (int) Math.ceil(horizonte / 6.0));
        int demandaPrevista = Math.max(baseline, baseline * fatorHorizonte);
        int duracaoPrevista = Math.max(15, demandaPrevista * 4);
        double confianca = Math.min(0.92, 0.55 + (Math.min(planos.size(), 100) / 270.0));

        return new PrevisaoDemandaYardDto(
                "HEURISTICO_HISTORICO",
                VERSAO_MODELO,
                true,
                false,
                horizonte,
                demandaPrevista,
                duracaoPrevista,
                confianca,
                baseline,
                demandaPrevista - baseline,
                "Estimativa baseada no volume de planos operacionais ativos e no horizonte solicitado. A sugestão não grava posições e permanece sujeita às regras determinísticas.",
                validacoesObrigatorias(),
                LocalDateTime.now());
    }

    private PrevisaoDemandaYardDto respostaFallback(int horizonte, int baseline, String motivo) {
        return new PrevisaoDemandaYardDto(
                "DETERMINISTICO",
                VERSAO_MODELO,
                modeloAtivo,
                true,
                horizonte,
                baseline,
                Math.max(15, baseline * 4),
                1.0,
                baseline,
                0,
                motivo + " O algoritmo determinístico foi usado integralmente.",
                validacoesObrigatorias(),
                LocalDateTime.now());
    }

    private List<String> validacoesObrigatorias() {
        return List.of(
                "capacidade e ocupação da posição",
                "restrições operacionais e de segurança",
                "compatibilidade da unidade com bloco e equipamento",
                "reservas, bloqueios e validade do plano");
    }
}
