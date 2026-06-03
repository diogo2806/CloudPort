package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TanktopCalculadorServico {

    public PressaoTanktopDto calcularPressao(BobinaManifesto bobina, SetorTanktop setor, double espessuraDunnageMm) {
        double larguraM = (bobina.getLarguraMm() != null ? bobina.getLarguraMm() : 2000.0) / 1000.0;
        double pesoT = (bobina.getPesoKg() != null ? bobina.getPesoKg() : 0.0) / 1000.0;
        double dunnageWidthM = (espessuraDunnageMm / 1000.0) * 3.0;
        double contactAreaM2 = 2.0 * dunnageWidthM * larguraM;
        double pressao = contactAreaM2 > 0 ? pesoT / contactAreaM2 : 0.0;
        double cap = setor.getCapacidadeTM2();

        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        if (pressao > cap) {
            violacoes.add(new ViolacaoEstivaDto(
                    "SOBRECARGA_TANKTOP",
                    String.format("Pressão %.2f t/m² excede a capacidade nominal %.2f t/m² no setor %s",
                            pressao, cap, setor.getNome()),
                    setor.getId(),
                    "PERIGO"));
        } else if (pressao > cap * 0.80) {
            violacoes.add(new ViolacaoEstivaDto(
                    "SOBRECARGA_TANKTOP",
                    String.format("Pressão %.2f t/m² está acima de 80%% da capacidade nominal %.2f t/m² no setor %s",
                            pressao, cap, setor.getNome()),
                    setor.getId(),
                    "AVISO"));
        }

        PressaoTanktopDto dto = new PressaoTanktopDto();
        dto.setSetorId(setor.getId());
        dto.setNomeSetor(setor.getNome());
        dto.setPressaoCalculadaTM2(Math.round(pressao * 100.0) / 100.0);
        dto.setCapacidadeNominalTM2(cap);
        dto.setPercentualOcupacao(Math.round(pressao / cap * 1000.0) / 10.0);
        dto.setExcedido(!violacoes.isEmpty() && violacoes.stream().anyMatch(v -> "PERIGO".equals(v.getSeveridade())));
        dto.setViolacoes(violacoes);
        return dto;
    }

    public List<PressaoTanktopDto> verificarTodosSetores(
            List<br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina> posicoes) {
        List<PressaoTanktopDto> resultado = new ArrayList<>();
        for (var pos : posicoes) {
            if (pos.getSetor() != null && pos.getBobina() != null) {
                resultado.add(calcularPressao(pos.getBobina(), pos.getSetor(), pos.getEspessuraDunnageMm()));
            }
        }
        return resultado;
    }
}
