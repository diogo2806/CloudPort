package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.ConflitoDireitoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.SequenciaOperacaoRtgDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.HeatmapOcupacaoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.NivelOcupacaoEnum;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico.PlanoReshuffflingDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/otimizacao-avancada")
public class OtimizacaoAvancadaPatioControlador {

    private final MapaOcupacaoServico mapaOcupacao;
    private final GerenciadorInterlockingRtgServico gerenciadorRtg;
    private final PredictiveReshuffflingServico predictiveReshuffling;

    public OtimizacaoAvancadaPatioControlador(MapaOcupacaoServico mapaOcupacao,
                                               GerenciadorInterlockingRtgServico gerenciadorRtg,
                                               PredictiveReshuffflingServico predictiveReshuffling) {
        this.mapaOcupacao = mapaOcupacao;
        this.gerenciadorRtg = gerenciadorRtg;
        this.predictiveReshuffling = predictiveReshuffling;
    }

    @GetMapping("/heatmap")
    public HeatmapOcupacaoDto consultarHeatmap() {
        return mapaOcupacao.gerarHeatmap();
    }

    @GetMapping("/nivel-ocupacao")
    public NivelOcupacaoEnum obterNivelOcupacao() {
        return mapaOcupacao.obterNivelOcupacao();
    }

    @GetMapping("/rtg/conflitos")
    public List<ConflitoDireitoDto> identificarConflitosRtg() {
        return gerenciadorRtg.identificarConflitosDetecto();
    }

    @GetMapping("/rtg/sequencia/{fila}")
    public SequenciaOperacaoRtgDto obterSequenciaRtg(@PathVariable("fila") Integer fila) {
        return gerenciadorRtg.obterSequenciaOtimizada(fila);
    }

    @PostMapping("/rtg/direito/{identificador}/{fila}")
    public boolean requisitarDireitoPassagem(@PathVariable("identificador") String identificadorRtg,
                                              @PathVariable("fila") Integer fila) {
        return gerenciadorRtg.requisitarDireitoDePassagem(identificadorRtg, fila);
    }

    @PostMapping("/rtg/liberar/{identificador}")
    public void liberarDireitoPassagem(@PathVariable("identificador") String identificadorRtg) {
        gerenciadorRtg.liberarDireitoDePassagem(identificadorRtg);
    }

    @GetMapping("/reshuffling/plano")
    public PlanoReshuffflingDto analisarReshufffling() {
        return predictiveReshuffling.analisarNecessidadeReshuffling();
    }

    @PostMapping("/reshuffling/executar")
    public PlanoReshuffflingDto executarReshufffling() {
        PlanoReshuffflingDto plano = predictiveReshuffling.analisarNecessidadeReshuffling();

        if (plano.isRecomendado()) {
            for (var candidato : plano.getConteinersParaReshuffling()) {
                predictiveReshuffling.executarReshuffflingConteiner(candidato);
            }
        }

        return plano;
    }
}
