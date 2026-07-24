package br.com.cloudport.servicoyard.patio.avisoestivagem.servico;

import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.AtribuicaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.AvisoResposta;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.CorrecaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.HistoricoResposta;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.RevalidacaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoEventoHistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.HistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio.AvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.repositorio.HistoricoAvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.servico.DetectorViolacaoEstivagemPatioServico.ViolacaoDetectada;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvisoEstivagemPatioServico {

    private static final Set<StatusAvisoEstivagemPatio> STATUS_ATIVOS = EnumSet.of(
            StatusAvisoEstivagemPatio.ABERTO,
            StatusAvisoEstivagemPatio.ATRIBUIDO,
            StatusAvisoEstivagemPatio.EM_CORRECAO,
            StatusAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO,
            StatusAvisoEstivagemPatio.REABERTO);

    private final AvisoEstivagemPatioRepositorio avisoRepositorio;
    private final HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio;
    private final DetectorViolacaoEstivagemPatioServico detector;

    public AvisoEstivagemPatioServico(
            AvisoEstivagemPatioRepositorio avisoRepositorio,
            HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio,
            DetectorViolacaoEstivagemPatioServico detector) {
        this.avisoRepositorio = avisoRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.detector = detector;
    }

    @Transactional(readOnly = true)
    public List<AvisoResposta> listar(
            StatusAvisoEstivagemPatio status,
            SeveridadeAvisoEstivagemPatio severidade,
            String responsavel,
            String bloco,
            String codigoUnidade) {
        return avisoRepositorio.findAllByOrderByAtualizadoEmDesc().stream()
                .filter(aviso -> status == null || aviso.getStatus() == status)
                .filter(aviso -> severidade == null || aviso.getSeveridade() == severidade)
                .filter(aviso -> corresponde(aviso.getResponsavel(), responsavel))
                .filter(aviso -> corresponde(aviso.getBloco(), bloco))
                .filter(aviso -> corresponde(aviso.getCodigoUnidade(), codigoUnidade))
                .map(AvisoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvisoResposta buscar(Long id) {
        return AvisoResposta.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResposta> listarHistorico(Long id) {
        buscarEntidade(id);
        return historicoRepositorio.findByAvisoIdOrderByCriadoEmAscIdAsc(id).stream()
                .map(HistoricoResposta::de)
                .toList();
    }

    @Transactional
    public List<AvisoResposta> revalidarInventario(String ator) {
        LocalDateTime agora = LocalDateTime.now();
        Map<String, ViolacaoDetectada> violacoes = detector.detectar();
        List<AvisoEstivagemPatio> existentes = avisoRepositorio.findAll();

        for (ViolacaoDetectada violacao : violacoes.values()) {
            AvisoEstivagemPatio aviso = existentes.stream()
                    .filter(item -> Objects.equals(item.getChaveEstavel(), violacao.chaveEstavel()))
                    .findFirst()
                    .orElse(null);
            if (aviso == null) {
                abrir(violacao, ator, agora);
                continue;
            }
            atualizarDadosDetectados(aviso, violacao, agora);
            if (aviso.getStatus() == StatusAvisoEstivagemPatio.RESOLVIDO) {
                reabrir(aviso, ator, agora);
            } else if (aviso.getStatus() == StatusAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO) {
                falharRevalidacao(aviso, ator, agora);
            } else {
                AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
                registrarHistorico(
                        salvo,
                        TipoEventoHistoricoAvisoEstivagemPatio.ATUALIZACAO_AUTOMATICA,
                        salvo.getStatus(),
                        salvo.getStatus(),
                        ator,
                        "Revalidação automática confirmou que a violação continua ativa.",
                        salvo.getEvidencia(),
                        salvo.getResultado());
            }
        }

        for (AvisoEstivagemPatio aviso : existentes) {
            if (STATUS_ATIVOS.contains(aviso.getStatus()) && !violacoes.containsKey(aviso.getChaveEstavel())) {
                resolver(aviso, ator, agora, "A condição física deixou de violar a regra após revalidação do inventário.");
            }
        }
        return listar(null, null, null, null, null);
    }

    @Transactional
    public AvisoResposta atribuir(Long id, AtribuicaoRequisicao requisicao, String ator) {
        AvisoEstivagemPatio aviso = buscarAtivo(id);
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        aviso.setResponsavel(limitar(requisicao.getResponsavel(), 120));
        aviso.setPrazo(requisicao.getPrazo());
        aviso.setStatus(StatusAvisoEstivagemPatio.ATRIBUIDO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.ATRIBUICAO,
                anterior,
                salvo.getStatus(),
                ator,
                "Aviso atribuído a " + salvo.getResponsavel() + ".",
                null,
                null);
        return AvisoResposta.de(salvo);
    }

    @Transactional
    public AvisoResposta iniciarCorrecao(Long id, CorrecaoRequisicao requisicao, String ator) {
        AvisoEstivagemPatio aviso = buscarAtivo(id);
        if (!StringUtils.hasText(aviso.getResponsavel())) {
            aviso.setResponsavel(usuario(ator));
        }
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        aviso.setAcaoCorretiva(limitar(requisicao.getAcaoCorretiva(), 2000));
        aviso.setEvidencia(limitarOpcional(requisicao.getEvidencia(), 2000));
        aviso.setStatus(StatusAvisoEstivagemPatio.EM_CORRECAO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.CORRECAO_INICIADA,
                anterior,
                salvo.getStatus(),
                ator,
                salvo.getAcaoCorretiva(),
                salvo.getEvidencia(),
                null);
        return AvisoResposta.de(salvo);
    }

    @Transactional
    public AvisoResposta aguardarRevalidacao(Long id, RevalidacaoRequisicao requisicao, String ator) {
        AvisoEstivagemPatio aviso = buscarAtivo(id);
        if (!StringUtils.hasText(aviso.getAcaoCorretiva())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registre a ação corretiva antes de solicitar a revalidação.");
        }
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        if (requisicao != null && StringUtils.hasText(requisicao.getEvidencia())) {
            aviso.setEvidencia(limitar(requisicao.getEvidencia(), 2000));
        }
        aviso.setStatus(StatusAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.ENVIO_REVALIDACAO,
                anterior,
                salvo.getStatus(),
                ator,
                "Correção concluída operacionalmente e enviada para nova leitura física.",
                salvo.getEvidencia(),
                null);
        return AvisoResposta.de(salvo);
    }

    @Transactional
    public AvisoResposta revalidar(Long id, RevalidacaoRequisicao requisicao, String ator) {
        AvisoEstivagemPatio aviso = buscarEntidade(id);
        if (requisicao != null && StringUtils.hasText(requisicao.getEvidencia())) {
            aviso.setEvidencia(limitar(requisicao.getEvidencia(), 2000));
        }
        Map<String, ViolacaoDetectada> violacoes = detector.detectar();
        ViolacaoDetectada violacao = violacoes.get(aviso.getChaveEstavel());
        LocalDateTime agora = LocalDateTime.now();
        if (violacao == null) {
            resolver(aviso, ator, agora, "Revalidação confirmou que a condição foi corrigida.");
        } else {
            atualizarDadosDetectados(aviso, violacao, agora);
            if (aviso.getStatus() == StatusAvisoEstivagemPatio.RESOLVIDO) {
                reabrir(aviso, ator, agora);
            } else {
                falharRevalidacao(aviso, ator, agora);
            }
        }
        return AvisoResposta.de(avisoRepositorio.findById(id).orElse(aviso));
    }

    @Transactional(readOnly = true)
    public void validarOperacaoPlanejada(OrdemTrabalhoPatio ordem) {
        if (ordem == null) {
            return;
        }
        AvisoEstivagemPatio bloqueio = avisoRepositorio
                .findBySeveridadeAndStatusIn(SeveridadeAvisoEstivagemPatio.CRITICA, STATUS_ATIVOS)
                .stream()
                .filter(aviso -> Objects.equals(aviso.getLinha(), ordem.getLinhaDestino()))
                .filter(aviso -> Objects.equals(aviso.getColuna(), ordem.getColunaDestino()))
                .filter(aviso -> iguais(aviso.getCamada(), ordem.getCamadaDestino()))
                .findFirst()
                .orElse(null);
        if (bloqueio != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Planejamento ou dispatch bloqueado pelo aviso crítico #" + bloqueio.getId()
                            + " (" + bloqueio.getRegra() + ") na posição "
                            + posicao(bloqueio) + ". Revalide a condição física antes de continuar.");
        }
    }

    private void abrir(ViolacaoDetectada violacao, String ator, LocalDateTime agora) {
        AvisoEstivagemPatio aviso = new AvisoEstivagemPatio();
        atualizarDadosDetectados(aviso, violacao, agora);
        aviso.setStatus(StatusAvisoEstivagemPatio.ABERTO);
        aviso.setAbertoEm(agora);
        aviso.setRecorrencias(0);
        AvisoEstivagemPatio salvo = avisoRepositorio.saveAndFlush(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.ABERTURA,
                null,
                salvo.getStatus(),
                ator,
                salvo.getDescricao(),
                null,
                "Violação detectada no inventário físico.");
    }

    private void reabrir(AvisoEstivagemPatio aviso, String ator, LocalDateTime agora) {
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        aviso.setStatus(StatusAvisoEstivagemPatio.REABERTO);
        aviso.setAbertoEm(agora);
        aviso.setResolvidoEm(null);
        aviso.setResultado("A violação voltou a ocorrer após resolução anterior.");
        aviso.setRecorrencias((aviso.getRecorrencias() == null ? 0 : aviso.getRecorrencias()) + 1);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.REABERTURA,
                anterior,
                salvo.getStatus(),
                ator,
                "Recorrência detectada para a mesma unidade, posição e regra.",
                salvo.getEvidencia(),
                salvo.getResultado());
    }

    private void falharRevalidacao(AvisoEstivagemPatio aviso, String ator, LocalDateTime agora) {
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        aviso.setStatus(StatusAvisoEstivagemPatio.EM_CORRECAO);
        aviso.setUltimaRevalidacaoEm(agora);
        aviso.setResultado("A condição permanece após a revalidação.");
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.REVALIDACAO_FALHOU,
                anterior,
                salvo.getStatus(),
                ator,
                salvo.getDescricao(),
                salvo.getEvidencia(),
                salvo.getResultado());
    }

    private void resolver(AvisoEstivagemPatio aviso, String ator, LocalDateTime agora, String resultado) {
        StatusAvisoEstivagemPatio anterior = aviso.getStatus();
        aviso.setStatus(StatusAvisoEstivagemPatio.RESOLVIDO);
        aviso.setUltimaRevalidacaoEm(agora);
        aviso.setResolvidoEm(agora);
        aviso.setResultado(limitar(resultado, 2000));
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(
                salvo,
                TipoEventoHistoricoAvisoEstivagemPatio.RESOLUCAO,
                anterior,
                salvo.getStatus(),
                ator,
                "Aviso encerrado exclusivamente por revalidação física sem violação ativa.",
                salvo.getEvidencia(),
                salvo.getResultado());
    }

    private void atualizarDadosDetectados(
            AvisoEstivagemPatio aviso,
            ViolacaoDetectada violacao,
            LocalDateTime agora) {
        aviso.setChaveEstavel(violacao.chaveEstavel());
        aviso.setCodigoUnidade(violacao.codigoUnidade());
        aviso.setPosicaoId(violacao.posicaoId());
        aviso.setBloco(violacao.bloco());
        aviso.setLinha(violacao.linha());
        aviso.setColuna(violacao.coluna());
        aviso.setCamada(violacao.camada());
        aviso.setRegra(violacao.regra());
        aviso.setSeveridade(violacao.severidade());
        aviso.setDescricao(limitar(violacao.descricao(), 1000));
        aviso.setValorObservado(limitarOpcional(violacao.valorObservado(), 1000));
        aviso.setValorEsperado(limitarOpcional(violacao.valorEsperado(), 1000));
        aviso.setAcaoSugerida(limitarOpcional(violacao.acaoSugerida(), 1000));
        aviso.setUltimaRevalidacaoEm(agora);
    }

    private void registrarHistorico(
            AvisoEstivagemPatio aviso,
            TipoEventoHistoricoAvisoEstivagemPatio tipoEvento,
            StatusAvisoEstivagemPatio statusAnterior,
            StatusAvisoEstivagemPatio statusNovo,
            String ator,
            String detalhes,
            String evidencia,
            String resultado) {
        HistoricoAvisoEstivagemPatio historico = new HistoricoAvisoEstivagemPatio();
        historico.setAviso(aviso);
        historico.setTipoEvento(tipoEvento);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setAtor(limitar(usuario(ator), 120));
        historico.setDetalhes(limitarOpcional(detalhes, 2000));
        historico.setEvidencia(limitarOpcional(evidencia, 2000));
        historico.setResultado(limitarOpcional(resultado, 2000));
        historicoRepositorio.save(historico);
    }

    private AvisoEstivagemPatio buscarEntidade(Long id) {
        return avisoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aviso de estivagem do pátio não encontrado."));
    }

    private AvisoEstivagemPatio buscarAtivo(Long id) {
        AvisoEstivagemPatio aviso = buscarEntidade(id);
        if (!STATUS_ATIVOS.contains(aviso.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O aviso já está resolvido.");
        }
        return aviso;
    }

    private boolean corresponde(String valor, String filtro) {
        return !StringUtils.hasText(filtro)
                || (StringUtils.hasText(valor)
                && valor.toUpperCase(Locale.ROOT).contains(filtro.trim().toUpperCase(Locale.ROOT)));
    }

    private boolean iguais(String primeiro, String segundo) {
        return StringUtils.hasText(primeiro)
                && StringUtils.hasText(segundo)
                && primeiro.trim().equalsIgnoreCase(segundo.trim());
    }

    private String posicao(AvisoEstivagemPatio aviso) {
        return "L" + aviso.getLinha() + "/C" + aviso.getColuna() + "/" + aviso.getCamada();
    }

    private String usuario(String ator) {
        return StringUtils.hasText(ator) ? ator.trim() : "SISTEMA";
    }

    private String limitar(String valor, int limite) {
        String limpo = StringUtils.hasText(valor) ? valor.trim() : "Não informado";
        return limpo.length() <= limite ? limpo : limpo.substring(0, limite);
    }

    private String limitarOpcional(String valor, int limite) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.length() <= limite ? limpo : limpo.substring(0, limite);
    }
}
