package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.ConflitoDireitoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.SequenciaOperacaoRtgDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.HeatmapOcupacaoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.NivelOcupacaoEnum;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico.PlanoReshuffflingDto;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/otimizacao-avancada")
public class OtimizacaoAvancadaPatioControlador {

    private final MapaOcupacaoServico mapaOcupacao;
    private final GerenciadorInterlockingRtgServico gerenciadorRtg;
    private final PredictiveReshuffflingServico predictiveReshuffling;
    private final AuditoriaComandoPatioServico auditoriaComandoPatioServico;

    public OtimizacaoAvancadaPatioControlador(MapaOcupacaoServico mapaOcupacao,
                                               GerenciadorInterlockingRtgServico gerenciadorRtg,
                                               PredictiveReshuffflingServico predictiveReshuffling,
                                               AuditoriaComandoPatioServico auditoriaComandoPatioServico) {
        this.mapaOcupacao = mapaOcupacao;
        this.gerenciadorRtg = gerenciadorRtg;
        this.predictiveReshuffling = predictiveReshuffling;
        this.auditoriaComandoPatioServico = auditoriaComandoPatioServico;
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
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public boolean requisitarDireitoPassagem(@PathVariable("identificador") String identificadorRtg,
                                              @PathVariable("fila") Integer fila) {
        return gerenciadorRtg.requisitarDireitoDePassagem(identificadorRtg, fila);
    }

    @PostMapping("/rtg/liberar/{identificador}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public void liberarDireitoPassagem(@PathVariable("identificador") String identificadorRtg) {
        gerenciadorRtg.liberarDireitoDePassagem(identificadorRtg);
    }

    @GetMapping("/reshuffling/plano")
    public PlanoReshuffflingDto analisarReshufffling() {
        return predictiveReshuffling.analisarNecessidadeReshuffling();
    }

    @PostMapping("/reshuffling/executar")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
    public PlanoReshuffflingDto executarReshufffling(
            @Valid @RequestBody ComandoMotivadoDto comando) {
        PlanoReshuffflingDto plano = predictiveReshuffling.analisarNecessidadeReshuffling();

        if (plano.isRecomendado()) {
            for (PredictiveReshuffflingServico.ConteinerParaReshuffflingDto candidato
                    : plano.getConteinersParaReshuffling()) {
                predictiveReshuffling.executarReshuffflingConteiner(candidato);
            }
        }

        auditoriaComandoPatioServico.registrar(
                null,
                null,
                "RESHUFFLING_EXECUTADO_COM_MOTIVO",
                comando,
                "Recomendado=" + plano.isRecomendado()
                        + ", candidatos=" + plano.getConteinersParaReshuffling().size() + "."
        );
        return plano;
    }
}
