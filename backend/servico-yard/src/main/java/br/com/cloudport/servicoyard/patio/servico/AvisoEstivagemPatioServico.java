package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ResumoAvisosEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.dto.ViolacaoEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.HistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.AvisoEstivagemPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.HistoricoAvisoEstivagemPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AvisoEstivagemPatioServico {

    private static final EnumSet<EstadoAvisoEstivagemPatio> ESTADOS_ATIVOS = EnumSet.of(
            EstadoAvisoEstivagemPatio.ABERTO,
            EstadoAvisoEstivagemPatio.ATRIBUIDO,
            EstadoAvisoEstivagemPatio.EM_CORRECAO,
            EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO,
            EstadoAvisoEstivagemPatio.REABERTO);
    private static final Pattern POSICAO_PATTERN = Pattern.compile("(\\d++)\\D++(\\d++)(?:\\D++(\\d++))?");
    private static final String ATOR_SISTEMA = "sistema-yard";

    private final AvisoEstivagemPatioRepositorio avisoRepositorio;
    private final HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final DetectorAvisoEstivagemPatioServico detectorServico;

    public AvisoEstivagemPatioServico(AvisoEstivagemPatioRepositorio avisoRepositorio,
                                       HistoricoAvisoEstivagemPatioRepositorio historicoRepositorio,
                                       ConteinerPatioRepositorio conteinerRepositorio,
                                       DetectorAvisoEstivagemPatioServico detectorServico) {
        this.avisoRepositorio = avisoRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.detectorServico = detectorServico;
    }

    public List<AvisoEstivagemPatio> sincronizarInventario(String ator) {
        List<ConteinerPatio> inventario = carregarInventario();
        for (ConteinerPatio unidade : inventario) {
            if (unidade.getPosicao() != null) {
                reconciliar(unidade, inventario, ator, "Varredura do inventário físico");
            }
        }
        return listar(null, null, null, false);
    }

    public List<AvisoEstivagemPatio> reavaliarUnidade(String codigoUnidade, String ator) {
        ConteinerPatio unidade = conteinerRepositorio.findByCodigoIgnoreCase(
                        obrigatorio(codigoUnidade, "Código da unidade"))
                .orElseThrow(() -> new NoSuchElementException("Unidade não encontrada no pátio"));
        List<ConteinerPatio> inventario = carregarInventario();
        reconciliar(unidade, inventario, ator, "Revalidação da unidade");
        return avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(unidade.getCodigo(), ESTADOS_ATIVOS);
    }

    public void reavaliarAposMovimentacao(String codigoUnidade,
                                           String origem,
                                           String destino,
                                           String ator) {
        List<ConteinerPatio> inventario = carregarInventario();
        Set<Coordenada> referencias = new HashSet<>();
        coordenada(origem).ifPresent(referencias::add);
        coordenada(destino).ifPresent(referencias::add);
        String codigo = normalizarCodigo(codigoUnidade);

        inventario.stream()
                .filter(unidade -> unidade.getPosicao() != null)
                .filter(unidade -> normalizarCodigo(unidade.getCodigo()).equals(codigo)
                        || referencias.stream().anyMatch(referencia -> vizinha(unidade.getPosicao(), referencia)))
                .forEach(unidade -> reconciliar(
                        unidade,
                        inventario,
                        ator,
                        "Reavaliação automática de origem, destino e vizinhança após movimentação"));
    }

    public void validarOperacaoSemAvisoCritico(String codigoUnidade, String... posicoes) {
        if (StringUtils.hasText(codigoUnidade)) {
            conteinerRepositorio.findByCodigoIgnoreCase(codigoUnidade.trim())
                    .ifPresent(unidade -> reconciliar(
                            unidade,
                            carregarInventario(),
                            ATOR_SISTEMA,
                            "Revalidação preventiva antes do planejamento ou dispatch"));
        }

        boolean unidadeBloqueada = StringUtils.hasText(codigoUnidade)
                && avisoRepositorio.existsByCodigoUnidadeIgnoreCaseAndSeveridadeAndEstadoIn(
                        codigoUnidade.trim(), SeveridadeAvisoEstivagemPatio.CRITICA, ESTADOS_ATIVOS);
        Set<String> codigosPosicao = codigosPosicao(posicoes);
        boolean posicaoBloqueada = !codigosPosicao.isEmpty()
                && avisoRepositorio.existsByCodigoPosicaoInAndSeveridadeAndEstadoIn(
                        codigosPosicao, SeveridadeAvisoEstivagemPatio.CRITICA, ESTADOS_ATIVOS);
        if (unidadeBloqueada || posicaoBloqueada) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Operação bloqueada por aviso crítico de estivagem ativo. Corrija e revalide o caso antes de continuar.");
        }
    }

    public AvisoEstivagemPatio atribuir(Long id,
                                         String responsavel,
                                         LocalDateTime prazo,
                                         String ator) {
        AvisoEstivagemPatio aviso = obter(id);
        exigirAtivo(aviso);
        EstadoAvisoEstivagemPatio anterior = aviso.getEstado();
        aviso.setResponsavel(obrigatorio(responsavel, "Responsável"));
        aviso.setPrazo(prazo);
        aviso.setEstado(EstadoAvisoEstivagemPatio.ATRIBUIDO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.ATRIBUICAO, anterior,
                salvo.getEstado(), ator, "Aviso atribuído para tratamento", null, null);
        return salvo;
    }

    public AvisoEstivagemPatio iniciarCorrecao(Long id,
                                                String acaoCorretiva,
                                                String evidencia,
                                                String ator) {
        AvisoEstivagemPatio aviso = obter(id);
        exigirAtivo(aviso);
        if (aviso.getEstado() == EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO) {
            throw new IllegalStateException("O aviso já aguarda revalidação");
        }
        EstadoAvisoEstivagemPatio anterior = aviso.getEstado();
        aviso.setAcaoCorretiva(obrigatorio(acaoCorretiva, "Ação corretiva"));
        aviso.setEvidencia(acumular(aviso.getEvidencia(), evidencia));
        aviso.setEstado(EstadoAvisoEstivagemPatio.EM_CORRECAO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.INICIO_CORRECAO, anterior,
                salvo.getEstado(), ator, salvo.getAcaoCorretiva(), evidencia, null);
        return salvo;
    }

    public AvisoEstivagemPatio enviarParaRevalidacao(Long id,
                                                      String evidencia,
                                                      String ator) {
        AvisoEstivagemPatio aviso = obter(id);
        if (aviso.getEstado() != EstadoAvisoEstivagemPatio.EM_CORRECAO) {
            throw new IllegalStateException("O aviso deve estar em correção antes da revalidação");
        }
        EstadoAvisoEstivagemPatio anterior = aviso.getEstado();
        aviso.setEvidencia(acumular(aviso.getEvidencia(), evidencia));
        aviso.setEstado(EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO);
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.ENVIO_REVALIDACAO, anterior,
                salvo.getEstado(), ator, "Correção concluída; condição física ainda precisa ser revalidada",
                evidencia, null);
        return salvo;
    }

    public AvisoEstivagemPatio revalidar(Long id, String evidencia, String ator) {
        AvisoEstivagemPatio aviso = obter(id);
        EstadoAvisoEstivagemPatio estadoAntesDaRevalidacao = aviso.getEstado();
        ConteinerPatio unidade = aviso.getUnidade();
        List<ConteinerPatio> inventario = carregarInventario();
        reconciliar(unidade, inventario, ator, "Revalidação explícita do aviso #" + id);

        AvisoEstivagemPatio atualizado = obter(id);
        atualizado.setEvidencia(acumular(atualizado.getEvidencia(), evidencia));
        atualizado.setUltimaRevalidacaoEm(LocalDateTime.now());
        boolean resolvido = atualizado.getEstado() == EstadoAvisoEstivagemPatio.RESOLVIDO;
        atualizado.setResultadoRevalidacao(resolvido
                ? "A condição física deixou de existir após a revalidação."
                : "A condição física permanece presente; o aviso foi mantido ou reaberto.");
        AvisoEstivagemPatio salvo = avisoRepositorio.save(atualizado);
        registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.REVALIDACAO,
                estadoAntesDaRevalidacao, salvo.getEstado(), ator,
                "Revalidação explícita executada", evidencia, salvo.getResultadoRevalidacao());
        return salvo;
    }

    @Transactional(readOnly = true)
    public AvisoEstivagemPatio obter(Long id) {
        return avisoRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Aviso de estivagem não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<HistoricoAvisoEstivagemPatio> historico(Long id) {
        if (!avisoRepositorio.existsById(id)) {
            throw new NoSuchElementException("Aviso de estivagem não encontrado");
        }
        return historicoRepositorio.findByAvisoIdOrderByOcorridoEmAsc(id);
    }

    @Transactional(readOnly = true)
    public List<AvisoEstivagemPatio> listar(EstadoAvisoEstivagemPatio estado,
                                            SeveridadeAvisoEstivagemPatio severidade,
                                            String responsavel,
                                            boolean incluirResolvidos) {
        List<AvisoEstivagemPatio> origem;
        if (estado != null || incluirResolvidos) {
            origem = avisoRepositorio.findAll();
        } else {
            origem = avisoRepositorio.findAllByEstadoInOrderByAtualizadoEmDesc(ESTADOS_ATIVOS);
        }
        return origem.stream()
                .filter(aviso -> estado == null || aviso.getEstado() == estado)
                .filter(aviso -> severidade == null || aviso.getSeveridade() == severidade)
                .filter(aviso -> !StringUtils.hasText(responsavel)
                        || responsavel.trim().equalsIgnoreCase(valor(aviso.getResponsavel())))
                .sorted(Comparator.comparingInt((AvisoEstivagemPatio aviso) -> peso(aviso.getSeveridade())).reversed()
                        .thenComparing(aviso -> aviso.getPrazo() == null ? LocalDateTime.MAX : aviso.getPrazo())
                        .thenComparing(AvisoEstivagemPatio::getAtualizadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumoAvisosEstivagemPatioDto resumo() {
        List<AvisoEstivagemPatio> ativos = avisoRepositorio
                .findAllByEstadoInOrderByAtualizadoEmDesc(ESTADOS_ATIVOS);
        return new ResumoAvisosEstivagemPatioDto(
                ativos.size(),
                ativos.stream().filter(aviso -> aviso.getSeveridade() == SeveridadeAvisoEstivagemPatio.CRITICA).count(),
                agrupar(ativos, aviso -> valorPadrao(aviso.getBloco(), "SEM_BLOCO")),
                agrupar(ativos, aviso -> valorPadrao(aviso.getBloco(), "SEM_BLOCO")
                        + ":" + aviso.getLinha() + "/" + aviso.getColuna()),
                agrupar(ativos, AvisoEstivagemPatio::getCodigoPosicao),
                agrupar(ativos, AvisoEstivagemPatio::getCodigoUnidade));
    }

    private void reconciliar(ConteinerPatio unidade,
                              List<ConteinerPatio> inventario,
                              String ator,
                              String contexto) {
        if (unidade == null || unidade.getPosicao() == null) {
            return;
        }
        List<ViolacaoEstivagemPatioDto> violacoes = detectorServico.detectar(unidade, inventario);
        Set<String> chavesEncontradas = new HashSet<>();
        for (ViolacaoEstivagemPatioDto violacao : violacoes) {
            String chave = chaveEstavel(unidade, unidade.getPosicao(), violacao);
            chavesEncontradas.add(chave);
            persistirViolacao(unidade, unidade.getPosicao(), violacao, chave, ator, contexto);
        }

        List<AvisoEstivagemPatio> ativosDaUnidade = avisoRepositorio
                .findByCodigoUnidadeIgnoreCaseAndEstadoIn(unidade.getCodigo(), ESTADOS_ATIVOS);
        for (AvisoEstivagemPatio aviso : ativosDaUnidade) {
            if (!chavesEncontradas.contains(aviso.getChaveEstavel())) {
                resolverPorAusencia(aviso, ator, contexto);
            }
        }
    }

    private void persistirViolacao(ConteinerPatio unidade,
                                    PosicaoPatio posicao,
                                    ViolacaoEstivagemPatioDto violacao,
                                    String chave,
                                    String ator,
                                    String contexto) {
        AvisoEstivagemPatio aviso = avisoRepositorio.findComBloqueioByChaveEstavel(chave).orElse(null);
        if (aviso == null) {
            AvisoEstivagemPatio novo = new AvisoEstivagemPatio();
            novo.setChaveEstavel(chave);
            novo.setUnidade(unidade);
            novo.setPosicao(posicao);
            novo.setCodigoUnidade(normalizarCodigo(unidade.getCodigo()));
            preencherPosicao(novo, posicao);
            novo.setRegra(violacao.regra());
            novo.setEstado(EstadoAvisoEstivagemPatio.ABERTO);
            novo.setOcorrencias(1);
            atualizarDeteccao(novo, violacao);
            novo.setUltimaRevalidacaoEm(LocalDateTime.now());
            AvisoEstivagemPatio salvo = avisoRepositorio.save(novo);
            registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.ABERTURA, null,
                    salvo.getEstado(), ator, contexto, null, salvo.getValorObservado());
            return;
        }

        EstadoAvisoEstivagemPatio anterior = aviso.getEstado();
        boolean dadosAlterados = dadosAlterados(aviso, violacao);
        aviso.setUnidade(unidade);
        aviso.setPosicao(posicao);
        aviso.setCodigoUnidade(normalizarCodigo(unidade.getCodigo()));
        preencherPosicao(aviso, posicao);
        atualizarDeteccao(aviso, violacao);
        aviso.setUltimaRevalidacaoEm(LocalDateTime.now());

        TipoEventoAvisoEstivagemPatio evento = null;
        if (anterior == EstadoAvisoEstivagemPatio.RESOLVIDO) {
            aviso.setEstado(EstadoAvisoEstivagemPatio.REABERTO);
            aviso.setOcorrencias(aviso.getOcorrencias() + 1);
            aviso.setResolvidoEm(null);
            aviso.setResultadoRevalidacao("A mesma violação voltou a ocorrer.");
            evento = TipoEventoAvisoEstivagemPatio.REABERTURA;
        } else if (anterior == EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO) {
            aviso.setEstado(EstadoAvisoEstivagemPatio.REABERTO);
            aviso.setResultadoRevalidacao("A revalidação confirmou que a violação permanece.");
            evento = TipoEventoAvisoEstivagemPatio.REABERTURA;
        } else if (dadosAlterados) {
            evento = TipoEventoAvisoEstivagemPatio.ATUALIZACAO_DETECCAO;
        }

        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        if (evento != null) {
            registrarHistorico(salvo, evento, anterior, salvo.getEstado(), ator,
                    contexto, null, salvo.getValorObservado());
        }
    }

    private void resolverPorAusencia(AvisoEstivagemPatio aviso, String ator, String contexto) {
        EstadoAvisoEstivagemPatio anterior = aviso.getEstado();
        aviso.setEstado(EstadoAvisoEstivagemPatio.RESOLVIDO);
        aviso.setResolvidoEm(LocalDateTime.now());
        aviso.setUltimaRevalidacaoEm(LocalDateTime.now());
        aviso.setResultadoRevalidacao("A condição deixou de existir na fotografia atual do inventário.");
        AvisoEstivagemPatio salvo = avisoRepositorio.save(aviso);
        registrarHistorico(salvo, TipoEventoAvisoEstivagemPatio.RESOLUCAO, anterior,
                salvo.getEstado(), ator, contexto, salvo.getEvidencia(), salvo.getResultadoRevalidacao());
    }

    private void atualizarDeteccao(AvisoEstivagemPatio aviso, ViolacaoEstivagemPatioDto violacao) {
        aviso.setSeveridade(violacao.severidade());
        aviso.setValorObservado(limitar(violacao.valorObservado(), 1000));
        aviso.setValorEsperado(limitar(violacao.valorEsperado(), 1000));
        aviso.setAcaoSugerida(limitar(violacao.acaoSugerida(), 1000));
        aviso.setBloqueiaOperacao(violacao.bloqueiaOperacao());
    }

    private boolean dadosAlterados(AvisoEstivagemPatio aviso, ViolacaoEstivagemPatioDto violacao) {
        return aviso.getSeveridade() != violacao.severidade()
                || !Objects.equals(aviso.getValorObservado(), limitar(violacao.valorObservado(), 1000))
                || !Objects.equals(aviso.getValorEsperado(), limitar(violacao.valorEsperado(), 1000));
    }

    private void preencherPosicao(AvisoEstivagemPatio aviso, PosicaoPatio posicao) {
        aviso.setCodigoPosicao(codigoPosicao(posicao));
        aviso.setBloco(normalizar(posicao.getBloco()));
        aviso.setLinha(posicao.getLinha());
        aviso.setColuna(posicao.getColuna());
        aviso.setCamada(posicao.getCamadaOperacional());
    }

    private void registrarHistorico(AvisoEstivagemPatio aviso,
                                     TipoEventoAvisoEstivagemPatio evento,
                                     EstadoAvisoEstivagemPatio anterior,
                                     EstadoAvisoEstivagemPatio novo,
                                     String ator,
                                     String detalhes,
                                     String evidencia,
                                     String resultado) {
        HistoricoAvisoEstivagemPatio historico = new HistoricoAvisoEstivagemPatio();
        historico.setAviso(aviso);
        historico.setEvento(evento);
        historico.setEstadoAnterior(anterior);
        historico.setEstadoNovo(novo);
        historico.setAtor(normalizarAtor(ator));
        historico.setDetalhes(limitar(detalhes, 2000));
        historico.setEvidencia(limitar(evidencia, 2000));
        historico.setResultado(limitar(resultado, 2000));
        historicoRepositorio.save(historico);
    }

    private Map<String, Long> agrupar(Collection<AvisoEstivagemPatio> avisos,
                                      Function<AvisoEstivagemPatio, String> chave) {
        return avisos.stream().collect(Collectors.groupingBy(
                chave,
                LinkedHashMap::new,
                Collectors.counting()));
    }

    private List<ConteinerPatio> carregarInventario() {
        return conteinerRepositorio.findAllByOrderByCodigoAsc();
    }

    private Set<String> codigosPosicao(String... posicoes) {
        if (posicoes == null) {
            return Set.of();
        }
        Set<String> codigos = new HashSet<>();
        for (String posicao : posicoes) {
            coordenada(posicao).map(Coordenada::codigo).ifPresent(codigos::add);
        }
        return codigos;
    }

    private java.util.Optional<Coordenada> coordenada(String valor) {
        if (!StringUtils.hasText(valor)) {
            return java.util.Optional.empty();
        }
        Matcher matcher = POSICAO_PATTERN.matcher(valor);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        String camada = matcher.group(3) != null ? matcher.group(3) : "1";
        return java.util.Optional.of(new Coordenada(
                Integer.valueOf(matcher.group(1)),
                Integer.valueOf(matcher.group(2)),
                camada));
    }

    private boolean vizinha(PosicaoPatio posicao, Coordenada referencia) {
        return Math.abs(posicao.getLinha() - referencia.linha()) <= 1
                && Math.abs(posicao.getColuna() - referencia.coluna()) <= 1;
    }

    private String chaveEstavel(ConteinerPatio unidade,
                                 PosicaoPatio posicao,
                                 ViolacaoEstivagemPatioDto violacao) {
        return normalizarCodigo(unidade.getCodigo()) + "|" + codigoPosicao(posicao) + "|" + violacao.regra().name();
    }

    private String codigoPosicao(PosicaoPatio posicao) {
        return posicao.getLinha() + "/" + posicao.getColuna() + "/" + posicao.getCamadaOperacional();
    }

    private void exigirAtivo(AvisoEstivagemPatio aviso) {
        if (!ESTADOS_ATIVOS.contains(aviso.getEstado())) {
            throw new IllegalStateException("Aviso de estivagem não está ativo");
        }
    }

    private int peso(SeveridadeAvisoEstivagemPatio severidade) {
        if (severidade == SeveridadeAvisoEstivagemPatio.CRITICA) {
            return 4;
        }
        if (severidade == SeveridadeAvisoEstivagemPatio.ALTA) {
            return 3;
        }
        if (severidade == SeveridadeAvisoEstivagemPatio.MEDIA) {
            return 2;
        }
        return 1;
    }

    private String acumular(String atual, String novo) {
        if (!StringUtils.hasText(novo)) {
            return atual;
        }
        String acumulado = StringUtils.hasText(atual) ? atual + "\n" + novo.trim() : novo.trim();
        return limitar(acumulado, 2000);
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(campo + " deve ser informado");
        }
        return valor.trim();
    }

    private String normalizarAtor(String ator) {
        return StringUtils.hasText(ator) ? limitar(ator.trim(), 120) : ATOR_SISTEMA;
    }

    private String normalizarCodigo(String codigo) {
        return StringUtils.hasText(codigo) ? codigo.trim().toUpperCase(Locale.ROOT) : "SEM_UNIDADE";
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String valor(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "";
    }

    private String valorPadrao(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }

    private String limitar(String valor, int limite) {
        if (valor == null || valor.length() <= limite) {
            return valor;
        }
        return valor.substring(0, limite);
    }

    private record Coordenada(Integer linha, Integer coluna, String camada) {
        private String codigo() {
            return linha + "/" + coluna + "/" + camada;
        }
    }
}
