package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MovimentoConteinerService {

    public static final String STATUS_NO_YARD = "no_yard";
    public static final String STATUS_EM_TRANSITO_RAIL = "em_transito_rail";
    public static final String STATUS_SAIU_DO_PORTO = "saiu_do_porto";

    public static final String TIPO_ENTRADA_GATE = "ENTRADA_GATE";
    public static final String TIPO_SAIDA_GATE = "SAIDA_GATE";
    public static final String TIPO_ARMAZENAGEM_YARD = "ARMAZENAGEM_YARD";
    public static final String TIPO_MOVIMENTO_RAIL = "MOVIMENTO_RAIL";

    private final ConteinerLocalizacaoRepository localizacaoRepository;
    private final HistoricoMovimentoRepository historicoRepository;

    public MovimentoConteinerService(ConteinerLocalizacaoRepository localizacaoRepository,
                                     HistoricoMovimentoRepository historicoRepository) {
        this.localizacaoRepository = localizacaoRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public void registrarEntradaGate(String containerId, String responsavel) {
        registrarMovimento(containerId, STATUS_NO_YARD, "GATE", "ENTRADA",
                TIPO_ENTRADA_GATE, "GATE_ENTRADA", responsavel, null,
                "Entrada do conteiner confirmada no gate.");
    }

    @Transactional
    public void registrarSaidaGate(String containerId, String responsavel) {
        registrarMovimento(containerId, STATUS_SAIU_DO_PORTO, "GATE", "SAIDA",
                TIPO_SAIDA_GATE, "GATE_SAIDA", responsavel, null,
                "Saida do conteiner confirmada no gate.");
    }

    @Transactional
    public void registrarArmazenagemYard(String containerId,
                                         String zona,
                                         String posicao,
                                         String equipamento,
                                         String responsavel) {
        String localizacao = montarLocalizacaoYard(zona, posicao);
        registrarMovimento(containerId, STATUS_NO_YARD, zona, posicao,
                TIPO_ARMAZENAGEM_YARD, localizacao, responsavel, equipamento,
                "Armazenagem do conteiner confirmada no patio.");
    }

    @Transactional
    public void registrarMovimentoRail(String containerId,
                                       String origem,
                                       String destino,
                                       String equipamento,
                                       String responsavel) {
        String localizacao = montarTrechoRail(origem, destino);
        registrarMovimento(containerId, STATUS_EM_TRANSITO_RAIL, destino, null,
                TIPO_MOVIMENTO_RAIL, localizacao, responsavel, equipamento,
                "Movimento ferroviario do conteiner confirmado.");
    }

    private void registrarMovimento(String containerId,
                                    String status,
                                    String zona,
                                    String posicao,
                                    String tipo,
                                    String localizacao,
                                    String responsavel,
                                    String equipamento,
                                    String observacoes) {
        validarContainerId(containerId);
        LocalDateTime agora = LocalDateTime.now();

        ConteinerLocalizacao localizacaoAtual = localizacaoRepository.findByContainerId(containerId)
                .orElseGet(() -> criarLocalizacao(containerId));
        localizacaoAtual.setStatusAtual(status);
        localizacaoAtual.setZona(zona);
        localizacaoAtual.setPosicao(posicao);
        localizacaoAtual.setDataAtualizacao(agora);
        localizacaoRepository.save(localizacaoAtual);

        HistoricoMovimento historico = new HistoricoMovimento();
        historico.setContainerId(containerId);
        historico.setTimestamp(agora);
        historico.setTipo(tipo);
        historico.setLocalizacao(localizacao);
        historico.setResponsavel(normalizarOpcional(responsavel));
        historico.setEquipamentoUsado(normalizarOpcional(equipamento));
        historico.setObservacoes(observacoes);
        historicoRepository.save(historico);
    }

    private ConteinerLocalizacao criarLocalizacao(String containerId) {
        ConteinerLocalizacao localizacao = new ConteinerLocalizacao();
        localizacao.setContainerId(containerId);
        return localizacao;
    }

    private String montarLocalizacaoYard(String zona, String posicao) {
        if (StringUtils.hasText(zona) && StringUtils.hasText(posicao)) {
            return zona.trim() + "-" + posicao.trim();
        }
        if (StringUtils.hasText(zona)) {
            return zona.trim();
        }
        return StringUtils.hasText(posicao) ? posicao.trim() : "YARD";
    }

    private String montarTrechoRail(String origem, String destino) {
        String origemNormalizada = StringUtils.hasText(origem) ? origem.trim() : "ORIGEM_NAO_INFORMADA";
        String destinoNormalizado = StringUtils.hasText(destino) ? destino.trim() : "DESTINO_NAO_INFORMADO";
        return origemNormalizada + " -> " + destinoNormalizado;
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private void validarContainerId(String containerId) {
        if (!StringUtils.hasText(containerId)) {
            throw new IllegalArgumentException("containerId e obrigatorio para registrar movimento.");
        }
    }
}
