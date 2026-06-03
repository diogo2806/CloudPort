package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmpilhamentoBobinaServico {

    private static final double LARGURA_CORREDOR_MIN_M = 1.5;
    private static final double LARGURA_EMPILHADEIRA_M = 1.2;

    public AnaliseEmpilhamentoDto analisarEmpilhamento(PlanoEstivaBulk plano, Long poraoId) {
        List<PosicaoBobina> posicoes = plano.getPosicoes().stream()
                .filter(p -> p.getPorao() != null && p.getPorao().getId().equals(poraoId))
                .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                .toList();

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        int maxCamada = posicoes.stream().mapToInt(PosicaoBobina::getCamada).max().orElse(0);
        double alturaFinal = calcularAlturaFinal(posicoes);

        PoraoNavio porao = posicoes.isEmpty() ? null : posicoes.get(0).getPorao();
        if (porao != null && porao.getAlturaUtil() != null && alturaFinal > porao.getAlturaUtil()) {
            violacoes.add(new ViolacaoEstivaDto("ALTURA_EXCEDIDA",
                    String.format("Altura da pilha %.2f m excede a altura útil do porão %.2f m",
                            alturaFinal, porao.getAlturaUtil()),
                    poraoId, "PERIGO"));
        }

        verificarIntertravamento(posicoes, violacoes);

        double larguraCorredor = calcularLarguraCorredor(posicoes, porao);
        boolean corredorLivre = larguraCorredor >= LARGURA_CORREDOR_MIN_M;
        if (!corredorLivre) {
            violacoes.add(new ViolacaoEstivaDto("CORREDOR_BLOQUEADO",
                    String.format("Corredor de operação %.2f m menor que o mínimo de %.1f m",
                            larguraCorredor, LARGURA_CORREDOR_MIN_M),
                    poraoId, "PERIGO"));
        }

        AnaliseEmpilhamentoDto dto = new AnaliseEmpilhamentoDto();
        dto.setTotalCamadas(maxCamada);
        dto.setAlturaFinalM(Math.round(alturaFinal * 100.0) / 100.0);
        dto.setCorredorOperacaoLivre(corredorLivre);
        dto.setLarguraCorredorM(Math.round(larguraCorredor * 100.0) / 100.0);
        dto.setViolacoes(violacoes);
        dto.setDescricaoEmpilhamento(violacoes.isEmpty()
                ? maxCamada + " camada(s), altura " + dto.getAlturaFinalM() + " m — configuração válida"
                : maxCamada + " camada(s) — " + violacoes.size() + " violação(ões) detectada(s)");
        return dto;
    }

    public double calcularAlturaCamada(double diametroBase, double diametroCima) {
        double r1 = diametroBase / 2000.0;
        double r2 = diametroCima / 2000.0;
        return Math.sqrt(r2 * r2 + 2.0 * r1 * r2);
    }

    private double calcularAlturaFinal(List<PosicaoBobina> posicoes) {
        if (posicoes.isEmpty()) return 0.0;
        Map<String, List<PosicaoBobina>> stacks = posicoes.stream()
                .collect(Collectors.groupingBy(
                        p -> Math.round(p.getPosicaoX() * 10) + "_" + Math.round(p.getPosicaoY() * 10)));

        double maxAltura = 0.0;
        for (List<PosicaoBobina> pilha : stacks.values()) {
            double altura = calcularAlturaPilha(pilha);
            if (altura > maxAltura) maxAltura = altura;
        }
        return maxAltura;
    }

    private double calcularAlturaPilha(List<PosicaoBobina> pilha) {
        List<PosicaoBobina> ordenada = pilha.stream()
                .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                .toList();
        double altura = 0.0;
        for (int i = 0; i < ordenada.size(); i++) {
            BobinaManifesto b = ordenada.get(i).getBobina();
            if (b == null) continue;
            double d = b.getDiametroExternoMm() != null ? b.getDiametroExternoMm() : 1500.0;
            if (i == 0) {
                altura += d / 1000.0;
            } else {
                BobinaManifesto bAnterior = ordenada.get(i - 1).getBobina();
                double dAnterior = bAnterior != null && bAnterior.getDiametroExternoMm() != null
                        ? bAnterior.getDiametroExternoMm() : d;
                altura += calcularAlturaCamada(dAnterior, d);
            }
        }
        return altura;
    }

    private void verificarIntertravamento(List<PosicaoBobina> posicoes, List<ViolacaoEstivaDto> violacoes) {
        Map<String, List<PosicaoBobina>> stacks = posicoes.stream()
                .collect(Collectors.groupingBy(
                        p -> Math.round(p.getPosicaoX() * 10) + "_" + Math.round(p.getPosicaoY() * 10)));

        for (List<PosicaoBobina> pilha : stacks.values()) {
            List<PosicaoBobina> ordenada = pilha.stream()
                    .sorted(Comparator.comparingInt(PosicaoBobina::getCamada)).toList();
            for (int i = 1; i < ordenada.size(); i++) {
                BobinaManifesto cima = ordenada.get(i).getBobina();
                BobinaManifesto baixo = ordenada.get(i - 1).getBobina();
                if (cima == null || baixo == null) continue;
                double dCima = cima.getDiametroExternoMm() != null ? cima.getDiametroExternoMm() : 0;
                double dBaixo = baixo.getDiametroExternoMm() != null ? baixo.getDiametroExternoMm() : 1;
                if (dCima > dBaixo * 1.2) {
                    violacoes.add(new ViolacaoEstivaDto("INTERTRAVAMENTO_INVALIDO",
                            "Bobina " + cima.getCodigo() + " (Ø" + dCima + "mm) acima de bobina menor "
                                    + baixo.getCodigo() + " (Ø" + dBaixo + "mm) — intertravamento instável",
                            null, "PERIGO"));
                }
            }
        }
    }

    private double calcularLarguraCorredor(List<PosicaoBobina> posicoes, PoraoNavio porao) {
        if (porao == null || porao.getLargura() == null) return LARGURA_CORREDOR_MIN_M + 1.0;
        if (posicoes.isEmpty()) return porao.getLargura();

        double larguraTotalBobinas = posicoes.stream()
                .filter(p -> p.getCamada() == 1 && p.getBobina() != null)
                .mapToDouble(p -> p.getBobina().getDiametroExternoMm() != null
                        ? p.getBobina().getDiametroExternoMm() / 1000.0 : 0)
                .sum();
        return Math.max(0.0, porao.getLargura() - larguraTotalBobinas);
    }
}
