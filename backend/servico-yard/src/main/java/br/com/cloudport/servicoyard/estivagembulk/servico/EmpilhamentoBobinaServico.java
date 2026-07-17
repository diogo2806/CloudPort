package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmpilhamentoBobinaServico {

    private static final double LARGURA_CORREDOR_MIN_M = 1.5;
    private static final double ESPACAMENTO_FILEIRAS_MIN_MM = 100.0;
    private static final double ESPACAMENTO_FILEIRAS_MAX_MM = 150.0;

    public AnaliseEmpilhamentoDto analisarEmpilhamento(PlanoEstivaBulk plano, Long poraoId) {
        List<PosicaoBobina> posicoes = plano.getPosicoes().stream()
                .filter(posicao -> posicao.getPorao() != null && poraoId.equals(posicao.getPorao().getId()))
                .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                .toList();

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        PoraoNavio porao = localizarPorao(plano, poraoId);
        validarGeometriaPorao(porao, poraoId, violacoes);
        validarPosicoes(posicoes, porao, violacoes);
        verificarIntertravamento(posicoes, violacoes);
        verificarSequenciaCamadas(posicoes, violacoes);

        int maxCamada = posicoes.stream().mapToInt(PosicaoBobina::getCamada).max().orElse(0);
        double alturaFinal = calcularAlturaFinal(posicoes, violacoes);
        if (porao != null && porao.getAlturaUtil() != null && alturaFinal > porao.getAlturaUtil()) {
            violacoes.add(new ViolacaoEstivaDto("ALTURA_EXCEDIDA",
                    String.format("Altura da pilha %.2f m excede a altura útil do porão %.2f m",
                            alturaFinal, porao.getAlturaUtil()),
                    poraoId, "PERIGO"));
        }

        double larguraCorredor = calcularLarguraCorredor(posicoes, porao);
        boolean corredorLivre = porao != null && porao.getLargura() != null
                && larguraCorredor >= LARGURA_CORREDOR_MIN_M;
        if (porao != null && porao.getLargura() != null && !corredorLivre) {
            violacoes.add(new ViolacaoEstivaDto("CORREDOR_BLOQUEADO",
                    String.format("Corredor de operação %.2f m menor que o mínimo de %.1f m",
                            larguraCorredor, LARGURA_CORREDOR_MIN_M),
                    poraoId, "PERIGO"));
        }

        AnaliseEmpilhamentoDto dto = new AnaliseEmpilhamentoDto();
        dto.setTotalCamadas(maxCamada);
        dto.setAlturaFinalM(arredondar(alturaFinal));
        dto.setCorredorOperacaoLivre(corredorLivre);
        dto.setLarguraCorredorM(arredondar(larguraCorredor));
        dto.setViolacoes(violacoes);
        dto.setDescricaoEmpilhamento(violacoes.stream().noneMatch(this::perigo)
                ? maxCamada + " camada(s), altura " + dto.getAlturaFinalM() + " m — configuração válida"
                : maxCamada + " camada(s) — " + violacoes.size() + " violação(ões) detectada(s)");
        return dto;
    }

    public double calcularAlturaCamada(double diametroBase, double diametroCima) {
        if (diametroBase <= 0.0 || diametroCima <= 0.0) {
            throw new IllegalArgumentException("Diâmetros reais são obrigatórios para calcular a altura da camada");
        }
        double raioBase = diametroBase / 2000.0;
        double raioCima = diametroCima / 2000.0;
        return Math.sqrt(raioCima * raioCima + 2.0 * raioBase * raioCima);
    }

    private PoraoNavio localizarPorao(PlanoEstivaBulk plano, Long poraoId) {
        if (plano.getNavio() == null) {
            return null;
        }
        return plano.getNavio().getPoroes().stream()
                .filter(porao -> poraoId.equals(porao.getId()))
                .findFirst()
                .orElse(null);
    }

    private void validarGeometriaPorao(PoraoNavio porao, Long poraoId,
            List<ViolacaoEstivaDto> violacoes) {
        if (porao == null) {
            violacoes.add(new ViolacaoEstivaDto("PORAO_INEXISTENTE",
                    "O porão informado não pertence ao navio do plano", poraoId, "PERIGO"));
            return;
        }
        if (naoPositivo(porao.getComprimento()) || naoPositivo(porao.getLargura())
                || naoPositivo(porao.getAlturaUtil())) {
            violacoes.add(new ViolacaoEstivaDto("GEOMETRIA_PORAO_INCOMPLETA",
                    "Comprimento, largura e altura útil reais do porão são obrigatórios",
                    poraoId, "PERIGO"));
        }
    }

    private void validarPosicoes(List<PosicaoBobina> posicoes, PoraoNavio porao,
            List<ViolacaoEstivaDto> violacoes) {
        Set<String> coordenadas = new HashSet<>();
        for (PosicaoBobina posicao : posicoes) {
            Long referencia = posicao.getId();
            BobinaManifesto bobina = posicao.getBobina();
            if (bobina == null || naoPositivo(bobina.getDiametroExternoMm())
                    || naoPositivo(bobina.getLarguraMm()) || naoPositivo(bobina.getPesoKg())) {
                violacoes.add(new ViolacaoEstivaDto("DADOS_BOBINA_INCOMPLETOS",
                        "Peso, diâmetro externo e largura reais são obrigatórios para o empilhamento",
                        referencia, "PERIGO"));
            }
            if (posicao.getCamada() <= 0) {
                violacoes.add(new ViolacaoEstivaDto("CAMADA_INVALIDA",
                        "A camada deve ser maior que zero", referencia, "PERIGO"));
            }
            if (posicao.getPosicaoX() == null || posicao.getPosicaoY() == null) {
                violacoes.add(new ViolacaoEstivaDto("COORDENADA_AUSENTE",
                        "As coordenadas reais da bobina são obrigatórias", referencia, "PERIGO"));
                continue;
            }
            if (porao != null && porao.getComprimento() != null && porao.getLargura() != null
                    && (posicao.getPosicaoX() < 0.0 || posicao.getPosicaoX() > porao.getComprimento()
                    || posicao.getPosicaoY() < 0.0 || posicao.getPosicaoY() > porao.getLargura())) {
                violacoes.add(new ViolacaoEstivaDto("POSICAO_FORA_DO_PORAO",
                        "A coordenada informada está fora da geometria do porão", referencia, "PERIGO"));
            }
            String chave = chavePosicao(posicao);
            if (!coordenadas.add(chave)) {
                violacoes.add(new ViolacaoEstivaDto("POSICAO_DUPLICADA",
                        "Há mais de uma bobina na mesma coordenada e camada", referencia, "PERIGO"));
            }
            if (posicao.getEspacamentoFileirasMm() == null
                    || posicao.getEspacamentoFileirasMm() < ESPACAMENTO_FILEIRAS_MIN_MM
                    || posicao.getEspacamentoFileirasMm() > ESPACAMENTO_FILEIRAS_MAX_MM) {
                violacoes.add(new ViolacaoEstivaDto("ESPACAMENTO_FILEIRAS_INVALIDO",
                        "O espaçamento entre fileiras deve estar documentado entre 100 e 150 mm",
                        referencia, "PERIGO"));
            }
        }
    }

    private double calcularAlturaFinal(List<PosicaoBobina> posicoes,
            List<ViolacaoEstivaDto> violacoes) {
        Map<String, List<PosicaoBobina>> pilhas = posicoes.stream()
                .filter(posicao -> posicao.getPosicaoX() != null && posicao.getPosicaoY() != null)
                .collect(Collectors.groupingBy(this::chavePilha));

        double maxAltura = 0.0;
        for (List<PosicaoBobina> pilha : pilhas.values()) {
            double altura = calcularAlturaPilha(pilha, violacoes);
            maxAltura = Math.max(maxAltura, altura);
        }
        return maxAltura;
    }

    private double calcularAlturaPilha(List<PosicaoBobina> pilha,
            List<ViolacaoEstivaDto> violacoes) {
        List<PosicaoBobina> ordenada = pilha.stream()
                .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                .toList();
        double altura = 0.0;
        for (int indice = 0; indice < ordenada.size(); indice++) {
            PosicaoBobina atual = ordenada.get(indice);
            BobinaManifesto bobina = atual.getBobina();
            if (bobina == null || naoPositivo(bobina.getDiametroExternoMm())) {
                continue;
            }
            if (indice == 0) {
                altura += bobina.getDiametroExternoMm() / 1000.0;
            } else {
                BobinaManifesto anterior = ordenada.get(indice - 1).getBobina();
                if (anterior == null || naoPositivo(anterior.getDiametroExternoMm())) {
                    violacoes.add(new ViolacaoEstivaDto("APOIO_CAMADA_INCOMPLETO",
                            "Não foi possível comprovar o apoio da camada superior",
                            atual.getId(), "PERIGO"));
                    continue;
                }
                altura += calcularAlturaCamada(anterior.getDiametroExternoMm(), bobina.getDiametroExternoMm());
            }
        }
        return altura;
    }

    private void verificarIntertravamento(List<PosicaoBobina> posicoes,
            List<ViolacaoEstivaDto> violacoes) {
        Map<String, List<PosicaoBobina>> pilhas = posicoes.stream()
                .filter(posicao -> posicao.getPosicaoX() != null && posicao.getPosicaoY() != null)
                .collect(Collectors.groupingBy(this::chavePilha));

        for (List<PosicaoBobina> pilha : pilhas.values()) {
            List<PosicaoBobina> ordenada = pilha.stream()
                    .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                    .toList();
            for (int indice = 1; indice < ordenada.size(); indice++) {
                PosicaoBobina posicaoCima = ordenada.get(indice);
                PosicaoBobina posicaoBaixo = ordenada.get(indice - 1);
                BobinaManifesto cima = posicaoCima.getBobina();
                BobinaManifesto baixo = posicaoBaixo.getBobina();
                if (cima == null || baixo == null || naoPositivo(cima.getDiametroExternoMm())
                        || naoPositivo(baixo.getDiametroExternoMm())) {
                    continue;
                }
                if (cima.getDiametroExternoMm() > baixo.getDiametroExternoMm() * 1.2) {
                    violacoes.add(new ViolacaoEstivaDto("INTERTRAVAMENTO_INVALIDO",
                            "Bobina " + cima.getCodigo() + " acima de bobina com diâmetro insuficiente",
                            posicaoCima.getId(), "PERIGO"));
                }
                if (cima.getPesoKg() != null && baixo.getPesoKg() != null
                        && cima.getPesoKg() > baixo.getPesoKg()) {
                    violacoes.add(new ViolacaoEstivaDto("PESO_CAMADA_INVALIDO",
                            "A bobina mais pesada deve permanecer na camada inferior",
                            posicaoCima.getId(), "PERIGO"));
                }
            }
        }
    }

    private void verificarSequenciaCamadas(List<PosicaoBobina> posicoes,
            List<ViolacaoEstivaDto> violacoes) {
        Map<String, List<PosicaoBobina>> pilhas = posicoes.stream()
                .filter(posicao -> posicao.getPosicaoX() != null && posicao.getPosicaoY() != null)
                .collect(Collectors.groupingBy(this::chavePilha));
        for (List<PosicaoBobina> pilha : pilhas.values()) {
            List<PosicaoBobina> ordenada = pilha.stream()
                    .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                    .toList();
            for (int indice = 0; indice < ordenada.size(); indice++) {
                PosicaoBobina atual = ordenada.get(indice);
                int camadaEsperada = indice + 1;
                if (atual.getCamada() != camadaEsperada) {
                    violacoes.add(new ViolacaoEstivaDto("CAMADA_SEM_APOIO",
                            "A pilha possui lacuna de camada ou não começa na camada 1",
                            atual.getId(), "PERIGO"));
                }
                if (indice > 0) {
                    PosicaoBobina inferior = ordenada.get(indice - 1);
                    if (atual.getSequenciaDescarga() == null || inferior.getSequenciaDescarga() == null
                            || atual.getSequenciaDescarga() >= inferior.getSequenciaDescarga()) {
                        violacoes.add(new ViolacaoEstivaDto("SEQUENCIA_DESCARGA_INSEGURA",
                                "A bobina superior deve ser descarregada antes da bobina que a suporta",
                                atual.getId(), "PERIGO"));
                    }
                }
            }
        }
    }

    private double calcularLarguraCorredor(List<PosicaoBobina> posicoes, PoraoNavio porao) {
        if (porao == null || porao.getLargura() == null || posicoes.isEmpty()) {
            return 0.0;
        }
        double larguraTotalBobinas = posicoes.stream()
                .filter(posicao -> posicao.getCamada() == 1 && posicao.getBobina() != null
                        && posicao.getBobina().getDiametroExternoMm() != null)
                .mapToDouble(posicao -> posicao.getBobina().getDiametroExternoMm() / 1000.0)
                .sum();
        return Math.max(0.0, porao.getLargura() - larguraTotalBobinas);
    }

    private String chavePilha(PosicaoBobina posicao) {
        return Math.round(posicao.getPosicaoX() * 10.0) + "_" + Math.round(posicao.getPosicaoY() * 10.0);
    }

    private String chavePosicao(PosicaoBobina posicao) {
        return chavePilha(posicao) + "_" + posicao.getCamada();
    }

    private boolean naoPositivo(Double valor) {
        return valor == null || valor <= 0.0;
    }

    private boolean perigo(ViolacaoEstivaDto violacao) {
        return "PERIGO".equals(violacao.getSeveridade());
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
