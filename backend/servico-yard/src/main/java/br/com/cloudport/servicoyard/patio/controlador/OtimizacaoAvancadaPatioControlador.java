package br.com.cloudport.servicoyard.patio.controlador;

import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.ConflitoDireitoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorInterlockingRtgServico.SequenciaOperacaoRtgDto;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorZonasBufferServico;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorZonasBufferServico.AnaliseCorredoresDto;
import br.com.cloudport.servicoyard.patio.otimizacao.GerenciadorZonasBufferServico.ConfiguracaoBufferDto;
import br.com.cloudport.servicoyard.patio.otimizacao.IntegracaoCronogramaNavioServico;
import br.com.cloudport.servicoyard.patio.otimizacao.IntegracaoCronogramaNavioServico.AnaliseCapacidadeNavioDto;
import br.com.cloudport.servicoyard.patio.otimizacao.IntegracaoCronogramaNavioServico.AlertaOperacionalDto;
import br.com.cloudport.servicoyard.patio.otimizacao.IntegracaoCronogramaNavioServico.PriorizacaoRtgPorNavioDto;
import br.com.cloudport.servicoyard.patio.otimizacao.IntegracaoCronogramaNavioServico.SequenciaOperacaoRtgPorNavioDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.HeatmapOcupacaoDto;
import br.com.cloudport.servicoyard.patio.otimizacao.MapaOcupacaoServico.NivelOcupacaoEnum;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico;
import br.com.cloudport.servicoyard.patio.otimizacao.PredictiveReshuffflingServico.PlanoReshuffflingDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/otimizacao-avancada")
public class OtimizacaoAvancadaPatioControlador {

    private final MapaOcupacaoServico mapaOcupacao;
    private final GerenciadorInterlockingRtgServico gerenciadorRtg;
    private final PredictiveReshuffflingServico predictiveReshuffling;
    private final GerenciadorZonasBufferServico gerenciadorBuffer;
    private final IntegracaoCronogramaNavioServico integracaoCronograma;

    public OtimizacaoAvancadaPatioControlador(MapaOcupacaoServico mapaOcupacao,
                                               GerenciadorInterlockingRtgServico gerenciadorRtg,
                                               PredictiveReshuffflingServico predictiveReshuffling,
                                               GerenciadorZonasBufferServico gerenciadorBuffer,
                                               IntegracaoCronogramaNavioServico integracaoCronograma) {
        this.mapaOcupacao = mapaOcupacao;
        this.gerenciadorRtg = gerenciadorRtg;
        this.predictiveReshuffling = predictiveReshuffling;
        this.gerenciadorBuffer = gerenciadorBuffer;
        this.integracaoCronograma = integracaoCronograma;
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

    @GetMapping("/buffer/configuracao")
    public ConfiguracaoBufferDto obterConfiguracaoBuffer() {
        return gerenciadorBuffer.obterConfiguracaoBuffer();
    }

    @GetMapping("/buffer/corredores")
    public AnaliseCorredoresDto analisarCorredoresManobra() {
        return gerenciadorBuffer.analisarCorredoresManobra();
    }

    @GetMapping("/buffer/alertas")
    public List<GerenciadorZonasBufferServico.ZonaAlertaDto> obterAlertasZonas() {
        return gerenciadorBuffer.identificarZonasEmRisco();
    }

    @PostMapping("/buffer/reservar/{linha}/{coluna}")
    public void reservarZonaBuffer(@PathVariable("linha") Integer linha,
                                    @PathVariable("coluna") Integer coluna,
                                    @RequestParam(name = "motivo", defaultValue = "Manobra") String motivo) {
        gerenciadorBuffer.reservarZonaBuffer(linha, coluna, motivo);
    }

    @PostMapping("/buffer/liberar/{linha}/{coluna}")
    public void liberarZonaBuffer(@PathVariable("linha") Integer linha,
                                   @PathVariable("coluna") Integer coluna) {
        gerenciadorBuffer.liberarZonaBuffer(linha, coluna);
    }

    @GetMapping("/navio/priorizacao-rtg")
    public PriorizacaoRtgPorNavioDto calcularPriorizacaoRtgPorNavio(
            @RequestParam("dataPartida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataPartida) {
        return integracaoCronograma.calcularPriorizacaoRtgPorNavio(dataPartida);
    }

    @GetMapping("/navio/sequencia-otimizada")
    public List<SequenciaOperacaoRtgPorNavioDto> obterSequenciaOtimizadaPorNavio(
            @RequestParam("dataPartida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataPartida) {
        return integracaoCronograma.obterSequenciaOtimizadaPorNavio(dataPartida);
    }

    @GetMapping("/navio/analise-capacidade")
    public AnaliseCapacidadeNavioDto analisarCapacidadeParaNavio(
            @RequestParam("dataPartida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataPartida) {
        return integracaoCronograma.analisarCapacidadeParaNavio(dataPartida);
    }

    @GetMapping("/navio/alertas-operacionais")
    public List<AlertaOperacionalDto> identificarAlertasOperacionais(
            @RequestParam("dataPartida") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataPartida) {
        return integracaoCronograma.identificarAlertasOperacionais(dataPartida);
    }
}
