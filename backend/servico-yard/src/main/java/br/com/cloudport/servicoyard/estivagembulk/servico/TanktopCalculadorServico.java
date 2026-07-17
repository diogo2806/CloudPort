package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TanktopCalculadorServico {

    private static final int MINIMO_LINHAS_DUNNAGE = 2;

    public PressaoTanktopDto calcularPressao(PosicaoBobina posicao) {
        PressaoTanktopDto dto = calcularPressao(
                posicao.getBobina(),
                posicao.getSetor(),
                posicao.getQuantidadeLinhasDunnage(),
                posicao.getLarguraDunnageMm(),
                posicao.getComprimentoContatoDunnageMm(),
                posicao.getId());
        dto.setPosicaoId(posicao.getId());
        return dto;
    }

    public PressaoTanktopDto calcularPressao(BobinaManifesto bobina, SetorTanktop setor,
            Integer quantidadeLinhasDunnage, Double larguraDunnageMm,
            Double comprimentoContatoDunnageMm) {
        return calcularPressao(bobina, setor, quantidadeLinhasDunnage, larguraDunnageMm,
                comprimentoContatoDunnageMm, null);
    }

    private PressaoTanktopDto calcularPressao(BobinaManifesto bobina, SetorTanktop setor,
            Integer quantidadeLinhasDunnage, Double larguraDunnageMm,
            Double comprimentoContatoDunnageMm, Long referenciaId) {
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        PressaoTanktopDto dto = new PressaoTanktopDto();
        dto.setPosicaoId(referenciaId);

        if (setor == null) {
            violacoes.add(violacao("SETOR_TANKTOP_AUSENTE",
                    "A posição não possui setor de tank top definido", referenciaId));
            return finalizar(dto, 0.0, 0.0, 0.0, violacoes);
        }

        dto.setSetorId(setor.getId());
        dto.setNomeSetor(setor.getNome());

        if (bobina == null || bobina.getPesoKg() == null || bobina.getPesoKg() <= 0.0) {
            violacoes.add(violacao("PESO_BOBINA_AUSENTE",
                    "O peso real da bobina é obrigatório para validar o tank top", referenciaId));
        }
        if (bobina == null || bobina.getLarguraMm() == null || bobina.getLarguraMm() <= 0.0) {
            violacoes.add(violacao("LARGURA_BOBINA_AUSENTE",
                    "A largura real da bobina é obrigatória para validar o apoio", referenciaId));
        }
        if (setor.getCapacidadeTM2() == null || setor.getCapacidadeTM2() <= 0.0) {
            violacoes.add(violacao("CAPACIDADE_TANKTOP_AUSENTE",
                    "A capacidade nominal do setor de tank top é obrigatória", referenciaId));
        }
        if (quantidadeLinhasDunnage == null || quantidadeLinhasDunnage < MINIMO_LINHAS_DUNNAGE) {
            violacoes.add(violacao("DUNNAGE_INSUFICIENTE",
                    "São necessárias ao menos duas linhas de dunnage com dimensões reais", referenciaId));
        }
        if (larguraDunnageMm == null || larguraDunnageMm <= 0.0) {
            violacoes.add(violacao("LARGURA_DUNNAGE_AUSENTE",
                    "A largura real do dunnage é obrigatória", referenciaId));
        }
        if (comprimentoContatoDunnageMm == null || comprimentoContatoDunnageMm <= 0.0) {
            violacoes.add(violacao("CONTATO_DUNNAGE_AUSENTE",
                    "O comprimento efetivo de contato do dunnage é obrigatório", referenciaId));
        }
        if (bobina != null && bobina.getLarguraMm() != null && comprimentoContatoDunnageMm != null
                && comprimentoContatoDunnageMm > bobina.getLarguraMm()) {
            violacoes.add(violacao("CONTATO_DUNNAGE_INVALIDO",
                    "O comprimento de contato não pode exceder a largura da bobina", referenciaId));
        }

        if (possuiPerigo(violacoes)) {
            double capacidade = setor.getCapacidadeTM2() != null ? setor.getCapacidadeTM2() : 0.0;
            return finalizar(dto, 0.0, 0.0, capacidade, violacoes);
        }

        double areaContatoM2 = quantidadeLinhasDunnage
                * (larguraDunnageMm / 1000.0)
                * (comprimentoContatoDunnageMm / 1000.0);
        double pesoToneladas = bobina.getPesoKg() / 1000.0;
        double pressao = pesoToneladas / areaContatoM2;
        double capacidade = setor.getCapacidadeTM2();

        if (pressao > capacidade) {
            violacoes.add(new ViolacaoEstivaDto(
                    "SOBRECARGA_TANKTOP",
                    String.format("Pressão %.2f t/m² excede a capacidade nominal %.2f t/m² no setor %s",
                            pressao, capacidade, setor.getNome()),
                    referenciaId,
                    "PERIGO"));
        } else if (pressao > capacidade * 0.80) {
            violacoes.add(new ViolacaoEstivaDto(
                    "MARGEM_TANKTOP_REDUZIDA",
                    String.format("Pressão %.2f t/m² está acima de 80%% da capacidade nominal %.2f t/m² no setor %s",
                            pressao, capacidade, setor.getNome()),
                    referenciaId,
                    "AVISO"));
        }

        return finalizar(dto, areaContatoM2, pressao, capacidade, violacoes);
    }

    public List<PressaoTanktopDto> verificarTodosSetores(List<PosicaoBobina> posicoes) {
        List<PressaoTanktopDto> resultado = new ArrayList<>();
        for (PosicaoBobina posicao : posicoes) {
            resultado.add(calcularPressao(posicao));
        }
        return resultado;
    }

    private PressaoTanktopDto finalizar(PressaoTanktopDto dto, double areaContatoM2,
            double pressao, double capacidade, List<ViolacaoEstivaDto> violacoes) {
        dto.setAreaContatoM2(arredondar(areaContatoM2));
        dto.setPressaoCalculadaTM2(arredondar(pressao));
        dto.setCapacidadeNominalTM2(capacidade);
        dto.setPercentualOcupacao(capacidade > 0.0 ? arredondarUmaCasa(pressao / capacidade * 100.0) : 0.0);
        dto.setExcedido(possuiPerigo(violacoes));
        dto.setViolacoes(violacoes);
        return dto;
    }

    private ViolacaoEstivaDto violacao(String tipo, String descricao, Long referenciaId) {
        return new ViolacaoEstivaDto(tipo, descricao, referenciaId, "PERIGO");
    }

    private boolean possuiPerigo(List<ViolacaoEstivaDto> violacoes) {
        return violacoes.stream().anyMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private double arredondarUmaCasa(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
