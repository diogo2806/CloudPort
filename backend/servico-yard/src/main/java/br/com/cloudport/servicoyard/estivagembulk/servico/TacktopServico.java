package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.MaterialLashingDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TacktopServico {

    private static final double ANGULO_DEFAULT_GRAUS = 12.0;

    public TacktopDto calcularTacktop(PlanoEstivaBulk plano) {
        Map<String, List<PosicaoBobina>> stacks = plano.getPosicoes().stream()
                .filter(p -> p.getBobina() != null)
                .collect(Collectors.groupingBy(
                        p -> (p.getPorao() != null ? p.getPorao().getId() : 0) + "_"
                                + Math.round(p.getPosicaoX() * 10) + "_"
                                + Math.round(p.getPosicaoY() * 10)));

        List<PosicaoBobina> topLayer = new ArrayList<>();
        for (List<PosicaoBobina> pilha : stacks.values()) {
            pilha.stream()
                    .max(Comparator.comparingInt(PosicaoBobina::getCamada))
                    .ifPresent(topLayer::add);
        }

        double anguloGraus = ANGULO_DEFAULT_GRAUS;
        if (!topLayer.isEmpty()) {
            double dSum = topLayer.stream()
                    .filter(p -> p.getBobina().getDiametroExternoMm() != null)
                    .mapToDouble(p -> p.getBobina().getDiametroExternoMm())
                    .average().orElse(1500.0);
            double calculado = Math.toDegrees(Math.asin(Math.min(1.0, dSum / (2.0 * dSum))));
            anguloGraus = Math.max(ANGULO_DEFAULT_GRAUS, calculado);
        }

        for (PosicaoBobina pos : topLayer) {
            pos.setAnguloInclinacao(anguloGraus);
            pos.setTipoLashing(TipoLashing.CORRENTE);
        }

        List<MaterialLashingDto> materiais = new ArrayList<>();
        double pesoLashingTotal = 0.0;

        for (PosicaoBobina pos : topLayer) {
            BobinaManifesto b = pos.getBobina();
            double pesoT = b.getPesoKg() != null ? b.getPesoKg() / 1000.0 : 0;

            materiais.add(new MaterialLashingDto("CORRENTE", 2, 4.0, 8.0,
                    "Corrente de amarração p/ bobina " + b.getCodigo()));
            pesoLashingTotal += 2 * 8.0;

            materiais.add(new MaterialLashingDto("MADEIRA_CUNHA", 2, 0.5, 5.0,
                    "Calço de madeira p/ bobina " + b.getCodigo()));
            pesoLashingTotal += 2 * 5.0;

            if (pesoT > 15.0) {
                materiais.add(new MaterialLashingDto("CINTA_ACO", 2, 5.0, 12.0,
                        "Cinta de aço p/ bobina pesada " + b.getCodigo() + " (" + pesoT + "t)"));
                pesoLashingTotal += 2 * 12.0;
            }
        }

        plano.getMateriais().clear();
        for (MaterialLashingDto m : materiais) {
            MaterialLashingBulk mat = new MaterialLashingBulk();
            mat.setPlano(plano);
            try {
                mat.setTipo(TipoLashing.valueOf(m.getTipo()));
            } catch (IllegalArgumentException ignored) {
                mat.setTipo(TipoLashing.SEM_LASHING);
            }
            mat.setQuantidade(m.getQuantidade());
            mat.setComprimentoM(m.getComprimentoM());
            mat.setPesoUnitarioKg(m.getPesoUnitarioKg());
            mat.setDescricao(m.getDescricao());
            plano.getMateriais().add(mat);
        }

        TacktopDto dto = new TacktopDto();
        dto.setNumeroBobinasTopLayer(topLayer.size());
        dto.setAnguloInclinacaoGraus(Math.round(anguloGraus * 10.0) / 10.0);
        dto.setMateriaisNecessarios(materiais);
        dto.setPesoTotalLashingKg(Math.round(pesoLashingTotal * 10.0) / 10.0);
        dto.setObservacoes("Bobinas da camada superior posicionadas a " + dto.getAnguloInclinacaoGraus()
                + "° para travamento mecânico. Utilizar dunnage de madeira sob todas as camadas.");
        return dto;
    }
}
