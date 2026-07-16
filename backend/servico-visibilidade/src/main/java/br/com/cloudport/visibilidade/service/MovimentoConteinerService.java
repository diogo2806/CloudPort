package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.evento.EventoMovimentacaoTremConcluidaMensagem;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentoPatioMensagem;
import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    public void registrarEntradaGate(String eventoId, String containerId, String responsavel) {
        registrarMovimento(eventoId, containerId, STATUS_NO_YARD, "GATE", "ENTRADA",
                TIPO_ENTRADA_GATE, "GATE_ENTRADA", responsavel, null,
                "Entrada do conteiner confirmada no gate.", LocalDateTime.now());
    }

    @Transactional
    public void registrarSaidaGate(String eventoId, String containerId, String responsavel) {
        registrarMovimento(eventoId, containerId, STATUS_SAIU_DO_PORTO, "GATE", "SAIDA",
                TIPO_SAIDA_GATE, "GATE_SAIDA", responsavel, null,
                "Saida do conteiner confirmada no gate.", LocalDateTime.now());
    }

    @Transactional
    public void registrarArmazenagemYard(String eventoId,
                                         String containerId,
                                         String zona,
                                         String posicao,
                                         String equipamento,
                                         String responsavel) {
        String localizacao = montarLocalizacaoYard(zona, posicao);
        registrarMovimento(eventoId, containerId, STATUS_NO_YARD, zona, posicao,
                TIPO_ARMAZENAGEM_YARD, localizacao, responsavel, equipamento,
                "Armazenagem do conteiner confirmada no patio.", LocalDateTime.now());
    }

    @Transactional
    public void registrarMovimentoPatio(String eventoId, EventoMovimentoPatioMensagem evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Evento de movimento do patio nao pode ser nulo.");
        }

        String posicao = montarPosicaoPatio(
                evento.getLinha(), evento.getColuna(), evento.getCamadaOperacional());
        String tipo = valorOuPadrao(evento.getTipoMovimento(), TIPO_ARMAZENAGEM_YARD);
        String observacoes = montarObservacaoPatio(evento.getDescricao(), evento.getDestino());
        LocalDateTime timestamp = evento.getRegistradoEm() == null
                ? LocalDateTime.now()
                : evento.getRegistradoEm();

        registrarMovimento(eventoId, evento.getCodigoConteiner(), STATUS_NO_YARD, "YARD", posicao,
                tipo, montarLocalizacaoYard("YARD", posicao), "YARD", null,
                observacoes, timestamp);
    }

    @Transactional
    public void registrarMovimentoRail(String eventoId,
                                       String containerId,
                                       String origem,
                                       String destino,
                                       String equipamento,
                                       String responsavel) {
        String localizacao = montarTrechoRail(origem, destino);
        registrarMovimento(eventoId, containerId, STATUS_EM_TRANSITO_RAIL, destino, null,
                TIPO_MOVIMENTO_RAIL, localizacao, responsavel, equipamento,
                "Movimento ferroviario do conteiner confirmado.", LocalDateTime.now());
    }

    @Transactional
    public void registrarMovimentoFerroviario(
            String eventoId,
            EventoMovimentacaoTremConcluidaMensagem evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Evento ferroviario nao pode ser nulo.");
        }

        String localizacao = evento.getIdVisitaTrem() == null
                ? "FERROVIA"
                : "FERROVIA:VISITA:" + evento.getIdVisitaTrem();
        String tipo = valorOuPadrao(evento.getTipoMovimentacao(), TIPO_MOVIMENTO_RAIL);
        String observacoes = "ordem=" + valor(evento.getIdOrdemMovimentacao())
                + "; status=" + valorOuPadrao(evento.getStatusEvento(), "CONCLUIDO");

        registrarMovimento(eventoId, evento.getCodigoConteiner(), STATUS_EM_TRANSITO_RAIL,
                localizacao, null, tipo, localizacao, "RAIL", null,
                observacoes, converter(evento.getConcluidoEm()));
    }

    private void registrarMovimento(String eventoId,
                                    String containerId,
                                    String status,
                                    String zona,
                                    String posicao,
                                    String tipo,
                                    String localizacao,
                                    String responsavel,
                                    String equipamento,
                                    String observacoes,
                                    LocalDateTime timestamp) {
        validarEventoId(eventoId);
        validarContainerId(containerId);
        LocalDateTime ocorridoEm = timestamp == null ? LocalDateTime.now() : timestamp;

        ConteinerLocalizacao localizacaoAtual = localizacaoRepository.findByContainerId(containerId)
                .orElseGet(() -> criarLocalizacao(containerId));
        localizacaoAtual.setStatusAtual(status);
        localizacaoAtual.setZona(normalizarOpcional(zona));
        localizacaoAtual.setPosicao(normalizarOpcional(posicao));
        localizacaoAtual.setDataAtualizacao(ocorridoEm);
        localizacaoRepository.save(localizacaoAtual);

        HistoricoMovimento historico = new HistoricoMovimento();
        historico.setContainerId(containerId.trim());
        historico.setEventoId(eventoId.trim());
        historico.setTimestamp(ocorridoEm);
        historico.setTipo(tipo);
        historico.setLocalizacao(localizacao);
        historico.setResponsavel(normalizarOpcional(responsavel));
        historico.setEquipamentoUsado(normalizarOpcional(equipamento));
        historico.setObservacoes(normalizarOpcional(observacoes));
        historicoRepository.save(historico);
    }

    private ConteinerLocalizacao criarLocalizacao(String containerId) {
        ConteinerLocalizacao localizacao = new ConteinerLocalizacao();
        localizacao.setContainerId(containerId.trim());
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

    private String montarPosicaoPatio(Integer linha, Integer coluna, String camada) {
        if (linha == null || coluna == null) {
            return null;
        }

        StringBuilder posicao = new StringBuilder()
                .append("L").append(linha)
                .append("-C").append(coluna);
        if (StringUtils.hasText(camada)) {
            posicao.append("-").append(camada.trim());
        }
        return posicao.toString();
    }

    private String montarObservacaoPatio(String descricao, String destino) {
        String descricaoNormalizada = normalizarOpcional(descricao);
        String destinoNormalizado = normalizarOpcional(destino);
        if (descricaoNormalizada == null && destinoNormalizado == null) {
            return "Movimento de patio confirmado.";
        }
        if (destinoNormalizado == null) {
            return descricaoNormalizada;
        }
        if (descricaoNormalizada == null) {
            return "destino=" + destinoNormalizado;
        }
        return descricaoNormalizada + "; destino=" + destinoNormalizado;
    }

    private String montarTrechoRail(String origem, String destino) {
        String origemNormalizada = StringUtils.hasText(origem) ? origem.trim() : "ORIGEM_NAO_INFORMADA";
        String destinoNormalizado = StringUtils.hasText(destino) ? destino.trim() : "DESTINO_NAO_INFORMADO";
        return origemNormalizada + " -> " + destinoNormalizado;
    }

    private String valorOuPadrao(String valor, String padrao) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null ? padrao : normalizado;
    }

    private String valor(Object valor) {
        return valor == null ? "nao informado" : String.valueOf(valor);
    }

    private LocalDateTime converter(OffsetDateTime timestamp) {
        return timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private void validarEventoId(String eventoId) {
        if (!StringUtils.hasText(eventoId)) {
            throw new IllegalArgumentException("eventoId e obrigatorio para registrar movimento.");
        }
    }

    private void validarContainerId(String containerId) {
        if (!StringUtils.hasText(containerId)) {
            throw new IllegalArgumentException("containerId e obrigatorio para registrar movimento.");
        }
    }
}
