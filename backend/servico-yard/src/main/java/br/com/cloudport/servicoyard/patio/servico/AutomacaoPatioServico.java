package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.RespostaAutoplanejamentoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.OtimizadorPesquisaOperacionalPatioServico.ResultadoOtimizacao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AutomacaoPatioServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final PosicaoPatioRepositorio posicaoPatioRepositorio;
    private final OtimizadorPesquisaOperacionalPatioServico otimizador;

    public AutomacaoPatioServico(
            ConteinerPatioRepositorio conteinerPatioRepositorio,
            EquipamentoPatioRepositorio equipamentoPatioRepositorio,
            PosicaoPatioRepositorio posicaoPatioRepositorio,
            OtimizadorPesquisaOperacionalPatioServico otimizador
    ) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.posicaoPatioRepositorio = posicaoPatioRepositorio;
        this.otimizador = otimizador;
    }

    @Transactional
    public RespostaAutoplanejamentoDto executarAutoplanejamento() {
        List<ConteinerPatio> inventario = conteinerPatioRepositorio.findAll();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();
        List<PosicaoPatio> posicoes = posicaoPatioRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc();
        List<ConteinerPatio> candidatos = inventario.stream()
                .filter(this::ehCandidatoAAutoplanejamento)
                .toList();

        ResultadoOtimizacao resultado = otimizador.otimizar(
                candidatos,
                posicoes,
                equipamentos,
                inventario);
        List<ConteinerPatio> conteineresPlanejados = new ArrayList<>();
        List<String> containersPlanificados = new ArrayList<>();
        List<String> containersException = new ArrayList<>();

        for (Map.Entry<ConteinerPatio, PosicaoPatio> alocacao : resultado.getAlocacoes().entrySet()) {
            ConteinerPatio conteiner = alocacao.getKey();
            conteiner.setPosicao(alocacao.getValue());
            conteineresPlanejados.add(conteiner);
            containersPlanificados.add(conteiner.getCodigo());
        }
        resultado.getMotivosNaoAlocacao().forEach((conteiner, motivo) ->
                containersException.add(conteiner.getCodigo() + " (" + motivo + ")"));

        if (!conteineresPlanejados.isEmpty()) {
            conteinerPatioRepositorio.saveAll(conteineresPlanejados);
            conteinerPatioRepositorio.flush();
        }

        return new RespostaAutoplanejamentoDto(
                candidatos.size(),
                containersPlanificados.size(),
                containersException.size(),
                containersPlanificados,
                containersException
        );
    }

    private boolean ehCandidatoAAutoplanejamento(ConteinerPatio conteiner) {
        return conteiner != null
                && conteiner.getPosicao() == null
                && conteiner.getStatus() != StatusConteiner.LIBERADO
                && conteiner.getStatus() != StatusConteiner.DESPACHADO
                && StringUtils.hasText(conteiner.getCodigo());
    }
}
