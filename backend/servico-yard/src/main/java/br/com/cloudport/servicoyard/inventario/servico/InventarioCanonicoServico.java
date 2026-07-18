package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.inventario.dto.InventarioCanonicoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.ContagemInventarioFisico;
import br.com.cloudport.servicoyard.inventario.modelo.PrefixoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.modelo.VinculoEquipamento;
import br.com.cloudport.servicoyard.inventario.repositorio.ContagemInventarioFisicoRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.PrefixoEquipamentoInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.TipoEquipamentoInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.VinculoEquipamentoRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventarioCanonicoServico {

    private static final String TIPO_CONTEINER_PADRAO = "CTR-UNKNOWN";

    private static final Map<UnidadeInventario.EstadoUnidade, Set<UnidadeInventario.EstadoUnidade>> TRANSICOES;

    static {
        EnumMap<UnidadeInventario.EstadoUnidade, Set<UnidadeInventario.EstadoUnidade>> transicoes =
                new EnumMap<>(UnidadeInventario.EstadoUnidade.class);
        transicoes.put(UnidadeInventario.EstadoUnidade.PRE_AVISADA,
                Set.of(UnidadeInventario.EstadoUnidade.ATIVA,
                        UnidadeInventario.EstadoUnidade.NO_PATIO,
                        UnidadeInventario.EstadoUnidade.INATIVA));
        transicoes.put(UnidadeInventario.EstadoUnidade.ATIVA,
                Set.of(UnidadeInventario.EstadoUnidade.NO_PATIO,
                        UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                        UnidadeInventario.EstadoUnidade.INATIVA));
        transicoes.put(UnidadeInventario.EstadoUnidade.NO_PATIO,
                Set.of(UnidadeInventario.EstadoUnidade.EM_OPERACAO,
                        UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                        UnidadeInventario.EstadoUnidade.EMBARCADA,
                        UnidadeInventario.EstadoUnidade.LIBERADA,
                        UnidadeInventario.EstadoUnidade.INATIVA));
        transicoes.put(UnidadeInventario.EstadoUnidade.EM_OPERACAO,
                Set.of(UnidadeInventario.EstadoUnidade.NO_PATIO,
                        UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                        UnidadeInventario.EstadoUnidade.EMBARCADA,
                        UnidadeInventario.EstadoUnidade.DESEMBARCADA,
                        UnidadeInventario.EstadoUnidade.INATIVA));
        transicoes.put(UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                Set.of(UnidadeInventario.EstadoUnidade.NO_PATIO,
                        UnidadeInventario.EstadoUnidade.EMBARCADA,
                        UnidadeInventario.EstadoUnidade.DESEMBARCADA,
                        UnidadeInventario.EstadoUnidade.DESPACHADA));
        transicoes.put(UnidadeInventario.EstadoUnidade.EMBARCADA,
                Set.of(UnidadeInventario.EstadoUnidade.DESEMBARCADA,
                        UnidadeInventario.EstadoUnidade.DESPACHADA));
        transicoes.put(UnidadeInventario.EstadoUnidade.DESEMBARCADA,
                Set.of(UnidadeInventario.EstadoUnidade.NO_PATIO,
                        UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                        UnidadeInventario.EstadoUnidade.LIBERADA));
        transicoes.put(UnidadeInventario.EstadoUnidade.LIBERADA,
                Set.of(UnidadeInventario.EstadoUnidade.EM_TRANSITO,
                        UnidadeInventario.EstadoUnidade.DESPACHADA,
                        UnidadeInventario.EstadoUnidade.NO_PATIO));
        transicoes.put(UnidadeInventario.EstadoUnidade.DESPACHADA,
                Set.of(UnidadeInventario.EstadoUnidade.ATIVA,
                        UnidadeInventario.EstadoUnidade.APOSENTADA));
        transicoes.put(UnidadeInventario.EstadoUnidade.INATIVA,
                Set.of(UnidadeInventario.EstadoUnidade.ATIVA,
                        UnidadeInventario.EstadoUnidade.APOSENTADA));
        transicoes.put(UnidadeInventario.EstadoUnidade.APOSENTADA, Set.of());
        TRANSICOES = Map.copyOf(transicoes);
    }

    private final UnidadeInventarioRepositorio unidadeRepositorio;
    private final TipoEquipamentoInventarioRepositorio tipoRepositorio;
    private final PrefixoEquipamentoInventarioRepositorio prefixoRepositorio;
    private final VinculoEquipamentoRepositorio vinculoRepositorio;
    private final ContagemInventarioFisicoRepositorio contagemRepositorio;
    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public InventarioCanonicoServico(UnidadeInventarioRepositorio unidadeRepositorio,
                                     TipoEquipamentoInventarioRepositorio tipoRepositorio,
                                     PrefixoEquipamentoInventarioRepositorio prefixoRepositorio,
                                     VinculoEquipamentoRepositorio vinculoRepositorio,
                                     ContagemInventarioFisicoRepositorio contagemRepositorio,
                                     ConteinerPatioRepositorio conteinerPatioRepositorio,
                                     SanitizadorEntrada sanitizadorEntrada) {
        this.unidadeRepositorio = unidadeRepositorio;
        this.tipoRepositorio = tipoRepositorio;
        this.prefixoRepositorio = prefixoRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.contagemRepositorio = contagemRepositorio;
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public InventarioCanonicoDTO.DashboardInventarioResposta listar(String identificacao,
                                                                     TipoEquipamentoInventario.CategoriaEquipamento categoria,
                                                                     UnidadeInventario.EstadoUnidade estado,
                                                                     UnidadeInventario.CondicaoEquipamento condicao,
                                                                     String proprietario,
                                                                     String operador,
                                                                     Boolean somenteComHold,
                                                                     Boolean somenteReefer) {
        sincronizarConteinersLegados();
        String identificacaoNormalizada = normalizarOpcional(identificacao);
        String proprietarioNormalizado = normalizarOpcional(proprietario);
        String operadorNormalizado = normalizarOpcional(operador);

        List<InventarioCanonicoDTO.UnidadeResumo> unidades = unidadeRepositorio.findAllByOrderByIdentificacaoAsc()
                .stream()
                .filter(unidade -> identificacaoNormalizada == null
                        || unidade.getIdentificacao().toUpperCase(Locale.ROOT).contains(identificacaoNormalizada))
                .filter(unidade -> categoria == null || unidade.getCategoria() == categoria)
                .filter(unidade -> estado == null || unidade.getEstado() == estado)
                .filter(unidade -> condicao == null || unidade.getCondicao() == condicao)
                .filter(unidade -> contemIgnorandoCaixa(unidade.getProprietario(), proprietarioNormalizado))
                .filter(unidade -> contemIgnorandoCaixa(unidade.getOperador(), operadorNormalizado))
                .map(this::mapearResumo)
                .filter(unidade -> !Boolean.TRUE.equals(somenteComHold) || unidade.holdsAtivos() > 0)
                .filter(unidade -> !Boolean.TRUE.equals(somenteReefer) || unidade.refrigerado())
                .collect(Collectors.toList());

        List<ContagemInventarioFisico> divergencias = contagemRepositorio
                .findByStatusNotOrderByRegistradoEmDesc(ContagemInventarioFisico.StatusContagem.RESOLVIDA);

        return new InventarioCanonicoDTO.DashboardInventarioResposta(
                criarResumo(unidades, divergencias.size()),
                unidades);
    }

    @Transactional(readOnly = true)
    public InventarioCanonicoDTO.UnidadeDetalheResposta detalhar(Long unidadeId) {
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        InventarioCanonicoDTO.UnidadeResumo resumo = mapearResumo(unidade);
        TipoEquipamentoInventario tipo = unidade.getTipoEquipamento();
        List<InventarioCanonicoDTO.TipoEquipamentoResposta> equivalentes = StringUtils.hasText(tipo.getGrupoEquivalencia())
                ? tipoRepositorio.findByGrupoEquivalenciaIgnoreCaseAndAtivoTrueOrderByCodigoAsc(tipo.getGrupoEquivalencia())
                .stream()
                .filter(equivalente -> !Objects.equals(equivalente.getId(), tipo.getId()))
                .map(this::mapearTipo)
                .collect(Collectors.toList())
                : List.of();

        List<InventarioCanonicoDTO.VinculoResposta> vinculos = vinculoRepositorio
                .findByUnidadePrincipalIdOrUnidadeRelacionadaIdOrderByMontadoEmDesc(unidadeId, unidadeId)
                .stream()
                .map(this::mapearVinculo)
                .collect(Collectors.toList());

        return new InventarioCanonicoDTO.UnidadeDetalheResposta(
                resumo,
                mapearTipo(tipo),
                equivalentes,
                unidade.getLacres().stream().map(lacre -> new InventarioCanonicoDTO.LacreResposta(
                        lacre.getNumero(), lacre.getTipo(), lacre.getStatus(), lacre.getAnexadoEm(),
                        lacre.getRemovidoEm(), lacre.getResponsavel())).collect(Collectors.toList()),
                unidade.getDocumentos().stream().map(documento -> new InventarioCanonicoDTO.DocumentoResposta(
                        documento.getTipo(), documento.getNumero(), documento.getUri(), documento.getChecksum(),
                        documento.getStatus(), documento.getValidoAte(), documento.getRegistradoEm()))
                        .collect(Collectors.toList()),
                unidade.getAvarias().stream().map(avaria -> new InventarioCanonicoDTO.AvariaResposta(
                        avaria.getComponente(), avaria.getTipo(), avaria.getSeveridade(), avaria.getStatus(),
                        avaria.getDescricao(), avaria.getDetectadaEm(), avaria.getReparadaEm(),
                        avaria.getResponsavel())).collect(Collectors.toList()),
                unidade.getRestricoes().stream().map(restricao -> new InventarioCanonicoDTO.RestricaoResposta(
                        restricao.getTipo(), restricao.getCodigo(), restricao.getDescricao(),
                        restricao.getAutoridade(), restricao.isAtiva(), restricao.getValidoDe(),
                        restricao.getValidoAte(), restricao.getRegistradoEm())).collect(Collectors.toList()),
                unidade.getManutencoes().stream().map(manutencao -> new InventarioCanonicoDTO.ManutencaoResposta(
                        manutencao.getOrdemServico(), manutencao.getTipoServico(), manutencao.getFornecedor(),
                        manutencao.getStatus(), manutencao.getAbertaEm(), manutencao.getConcluidaEm(),
                        manutencao.getObservacoes())).collect(Collectors.toList()),
                unidade.getRegistrosReefer().stream().sorted(Comparator.comparing(
                                UnidadeInventario.ReeferRegistro::getLidoEm).reversed())
                        .map(registro -> new InventarioCanonicoDTO.ReeferResposta(
                                registro.getSetpointC(), registro.getTemperaturaSupplyC(),
                                registro.getTemperaturaReturnC(), registro.getUmidadePercentual(),
                                registro.getVentilacaoM3h(), registro.isLigado(), registro.getAlarme(),
                                registro.getLidoEm(), registro.getResponsavel())).collect(Collectors.toList()),
                vinculos,
                unidade.getHistoricoAtributos().stream().sorted(Comparator.comparing(
                                UnidadeInventario.HistoricoAtributoRegistro::getAlteradoEm).reversed())
                        .map(historico -> new InventarioCanonicoDTO.HistoricoAtributoResposta(
                                historico.getAtributo(), historico.getValorAnterior(), historico.getValorAtual(),
                                historico.getOrigem(), historico.getResponsavel(), historico.getAlteradoEm()))
                        .collect(Collectors.toList()),
                unidade.getObservacoes(),
                unidade.getCriadoEm());
    }

    @Transactional(readOnly = true)
    public List<InventarioCanonicoDTO.TipoEquipamentoResposta> listarTipos() {
        return tipoRepositorio.findAllByOrderByCategoriaAscCodigoAsc().stream()
                .map(this::mapearTipo)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventarioCanonicoDTO.TipoEquipamentoResposta criarTipo(
            InventarioCanonicoDTO.CriarTipoEquipamentoRequest request) {
        if (request == null || request.categoria() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Categoria do equipamento é obrigatória");
        }
        String codigo = obrigatorio(request.codigo(), "Código do tipo de equipamento é obrigatório")
                .toUpperCase(Locale.ROOT);
        if (tipoRepositorio.existsByCodigoIgnoreCase(codigo)) {
            throw erro(HttpStatus.CONFLICT, "Tipo de equipamento já cadastrado");
        }
        validarDimensao(request.comprimentoMm(), "Comprimento");
        validarDimensao(request.larguraMm(), "Largura");
        validarDimensao(request.alturaMm(), "Altura");
        validarNaoNegativo(request.taraKg(), "Tara");
        validarNaoNegativo(request.capacidadeKg(), "Capacidade");

        TipoEquipamentoInventario tipo = new TipoEquipamentoInventario();
        tipo.setCodigo(codigo);
        tipo.setDescricao(obrigatorio(request.descricao(), "Descrição do tipo de equipamento é obrigatória"));
        tipo.setCategoria(request.categoria());
        tipo.setCodigoIso(normalizarOpcional(request.codigoIso()));
        tipo.setComprimentoMm(request.comprimentoMm());
        tipo.setLarguraMm(request.larguraMm());
        tipo.setAlturaMm(request.alturaMm());
        tipo.setTaraKg(request.taraKg());
        tipo.setCapacidadeKg(request.capacidadeKg());
        tipo.setRefrigerado(request.refrigerado());
        tipo.setGrupoEquivalencia(normalizarOpcional(request.grupoEquivalencia()));
        tipo.setAtivo(true);
        return mapearTipo(tipoRepositorio.save(tipo));
    }

    @Transactional(readOnly = true)
    public List<InventarioCanonicoDTO.PrefixoResposta> listarPrefixos() {
        return prefixoRepositorio.findAllByOrderByPrefixoAsc().stream()
                .map(this::mapearPrefixo)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventarioCanonicoDTO.PrefixoResposta criarPrefixo(InventarioCanonicoDTO.CriarPrefixoRequest request) {
        if (request == null || request.categoria() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Categoria do prefixo é obrigatória");
        }
        String prefixo = obrigatorio(request.prefixo(), "Prefixo é obrigatório").toUpperCase(Locale.ROOT);
        if (!prefixo.matches("[A-Z0-9]{2,12}")) {
            throw erro(HttpStatus.BAD_REQUEST, "Prefixo deve possuir entre 2 e 12 caracteres alfanuméricos");
        }
        if (prefixoRepositorio.existsByPrefixoIgnoreCase(prefixo)) {
            throw erro(HttpStatus.CONFLICT, "Prefixo já cadastrado");
        }
        PrefixoEquipamentoInventario entidade = new PrefixoEquipamentoInventario();
        entidade.setPrefixo(prefixo);
        entidade.setProprietario(normalizarOpcional(request.proprietario()));
        entidade.setCategoria(request.categoria());
        entidade.setAtivo(true);
        return mapearPrefixo(prefixoRepositorio.save(entidade));
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta criarUnidade(
            InventarioCanonicoDTO.CriarUnidadeRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados da unidade são obrigatórios");
        }
        String identificacao = normalizarIdentificacao(request.identificacao());
        if (unidadeRepositorio.existsByIdentificacaoIgnoreCase(identificacao)) {
            throw erro(HttpStatus.CONFLICT, "Unidade já cadastrada");
        }
        TipoEquipamentoInventario tipo = localizarTipo(request.tipoEquipamentoCodigo());
        UnidadeInventario unidade = new UnidadeInventario();
        unidade.setIdentificacao(identificacao);
        unidade.setPrefixo(extrairPrefixo(identificacao));
        unidade.setTipoEquipamento(tipo);
        unidade.setCategoria(tipo.getCategoria());
        unidade.setEstado(request.estado() == null ? UnidadeInventario.EstadoUnidade.PRE_AVISADA : request.estado());
        unidade.setCondicao(request.condicao() == null
                ? UnidadeInventario.CondicaoEquipamento.OPERACIONAL
                : request.condicao());
        unidade.setStatusManutencao(UnidadeInventario.StatusManutencao.NAO_REQUERIDA);
        unidade.setProprietario(normalizarOpcional(request.proprietario()));
        unidade.setOperador(normalizarOpcional(request.operador()));
        unidade.setPosicaoAtual(normalizarOpcional(request.posicaoAtual()));
        unidade.setPosicaoPlanejada(normalizarOpcional(request.posicaoPlanejada()));
        validarNaoNegativo(request.pesoBrutoKg(), "Peso bruto");
        unidade.setPesoBrutoKg(request.pesoBrutoKg());
        unidade.setObservacoes(normalizarOpcional(request.observacoes()));
        registrarHistorico(unidade, "CRIACAO", null, unidade.getEstado().name(), request.origemAcao(), request.usuario());
        UnidadeInventario salva = unidadeRepositorio.save(unidade);
        garantirPrefixoCadastrado(salva);
        return detalhar(salva.getId());
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarEstado(
            Long unidadeId,
            InventarioCanonicoDTO.AtualizarEstadoRequest request) {
        if (request == null || request.estado() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Novo estado é obrigatório");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        UnidadeInventario.EstadoUnidade anterior = unidade.getEstado();
        if (anterior == request.estado()) {
            return detalhar(unidadeId);
        }
        if (!TRANSICOES.getOrDefault(anterior, Set.of()).contains(request.estado())) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Transição de " + anterior + " para " + request.estado() + " não é permitida");
        }
        if ((request.estado() == UnidadeInventario.EstadoUnidade.LIBERADA
                || request.estado() == UnidadeInventario.EstadoUnidade.DESPACHADA)
                && unidade.possuiHoldAtivo(LocalDateTime.now())) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unidade possui hold ativo e não pode ser liberada ou despachada");
        }
        unidade.setEstado(request.estado());
        registrarHistorico(unidade, "ESTADO", anterior.name(), request.estado().name(),
                request.origemAcao(), combinarResponsavelMotivo(request.usuario(), request.motivo()));
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarPropriedade(
            Long unidadeId,
            InventarioCanonicoDTO.AtualizarPropriedadeRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados de propriedade são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        String proprietarioAnterior = unidade.getProprietario();
        String operadorAnterior = unidade.getOperador();
        unidade.setProprietario(normalizarOpcional(request.proprietario()));
        unidade.setOperador(normalizarOpcional(request.operador()));
        registrarHistorico(unidade, "PROPRIETARIO", proprietarioAnterior, unidade.getProprietario(),
                request.origemAcao(), request.usuario());
        registrarHistorico(unidade, "OPERADOR", operadorAnterior, unidade.getOperador(),
                request.origemAcao(), request.usuario());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta atualizarPosicao(
            Long unidadeId,
            InventarioCanonicoDTO.AtualizarPosicaoRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados de posição são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        String atualAnterior = unidade.getPosicaoAtual();
        String planejadaAnterior = unidade.getPosicaoPlanejada();
        unidade.setPosicaoAtual(normalizarOpcional(request.posicaoAtual()));
        unidade.setPosicaoPlanejada(normalizarOpcional(request.posicaoPlanejada()));
        registrarHistorico(unidade, "POSICAO_ATUAL", atualAnterior, unidade.getPosicaoAtual(),
                request.origemAcao(), request.usuario());
        registrarHistorico(unidade, "POSICAO_PLANEJADA", planejadaAnterior, unidade.getPosicaoPlanejada(),
                request.origemAcao(), request.usuario());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarLacre(
            Long unidadeId,
            InventarioCanonicoDTO.LacreRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados do lacre são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        String numero = obrigatorio(request.numero(), "Número do lacre é obrigatório").toUpperCase(Locale.ROOT);
        boolean duplicadoAtivo = unidade.getLacres().stream()
                .anyMatch(lacre -> numero.equalsIgnoreCase(lacre.getNumero()) && lacre.getRemovidoEm() == null);
        if (duplicadoAtivo) {
            throw erro(HttpStatus.CONFLICT, "Lacre já está ativo na unidade");
        }
        unidade.getLacres().add(new UnidadeInventario.LacreRegistro(
                numero,
                normalizarOpcional(request.tipo()),
                StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase(Locale.ROOT) : "ATIVO",
                LocalDateTime.now(),
                null,
                normalizarOpcional(request.responsavel())));
        registrarHistorico(unidade, "LACRE", null, numero, "INVENTARIO", request.responsavel());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarDocumento(
            Long unidadeId,
            InventarioCanonicoDTO.DocumentoRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados do documento são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        unidade.getDocumentos().add(new UnidadeInventario.DocumentoRegistro(
                obrigatorio(request.tipo(), "Tipo do documento é obrigatório"),
                normalizarOpcional(request.numero()),
                normalizarOpcional(request.uri()),
                normalizarOpcional(request.checksum()),
                request.status() == null ? UnidadeInventario.StatusDocumento.PENDENTE : request.status(),
                request.validoAte(),
                LocalDateTime.now()));
        registrarHistorico(unidade, "DOCUMENTO", null, request.tipo(), "INVENTARIO", null);
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarAvaria(
            Long unidadeId,
            InventarioCanonicoDTO.AvariaRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados da avaria são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        UnidadeInventario.StatusAvaria status = request.status() == null
                ? UnidadeInventario.StatusAvaria.ABERTA
                : request.status();
        unidade.getAvarias().add(new UnidadeInventario.AvariaRegistro(
                obrigatorio(request.componente(), "Componente da avaria é obrigatório"),
                obrigatorio(request.tipo(), "Tipo da avaria é obrigatório"),
                obrigatorio(request.severidade(), "Severidade da avaria é obrigatória").toUpperCase(Locale.ROOT),
                status,
                normalizarOpcional(request.descricao()),
                LocalDateTime.now(),
                request.reparadaEm(),
                normalizarOpcional(request.responsavel())));
        if (status == UnidadeInventario.StatusAvaria.ABERTA
                || status == UnidadeInventario.StatusAvaria.EM_REPARO) {
            alterarCondicao(unidade, UnidadeInventario.CondicaoEquipamento.AVARIADO,
                    "AVARIA", request.responsavel());
        }
        registrarHistorico(unidade, "AVARIA", null,
                request.componente() + ":" + request.tipo(), "INVENTARIO", request.responsavel());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarRestricao(
            Long unidadeId,
            InventarioCanonicoDTO.RestricaoRequest request) {
        if (request == null || request.tipo() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Tipo da restrição é obrigatório");
        }
        if (request.validoDe() != null && request.validoAte() != null
                && request.validoAte().isBefore(request.validoDe())) {
            throw erro(HttpStatus.BAD_REQUEST, "Validade final não pode ser anterior à validade inicial");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        String codigo = obrigatorio(request.codigo(), "Código da restrição é obrigatório").toUpperCase(Locale.ROOT);
        unidade.getRestricoes().add(new UnidadeInventario.RestricaoRegistro(
                request.tipo(),
                codigo,
                normalizarOpcional(request.descricao()),
                normalizarOpcional(request.autoridade()),
                request.ativa() == null || request.ativa(),
                request.validoDe(),
                request.validoAte(),
                LocalDateTime.now()));
        registrarHistorico(unidade, request.tipo().name(), null, codigo, "INVENTARIO", request.autoridade());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta adicionarManutencao(
            Long unidadeId,
            InventarioCanonicoDTO.ManutencaoRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados de manutenção são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        UnidadeInventario.StatusManutencao status = request.status() == null
                ? UnidadeInventario.StatusManutencao.ABERTA
                : request.status();
        unidade.getManutencoes().add(new UnidadeInventario.ManutencaoRegistro(
                obrigatorio(request.ordemServico(), "Ordem de serviço é obrigatória"),
                obrigatorio(request.tipoServico(), "Tipo de serviço é obrigatório"),
                normalizarOpcional(request.fornecedor()),
                status,
                LocalDateTime.now(),
                request.concluidaEm(),
                normalizarOpcional(request.observacoes())));
        UnidadeInventario.StatusManutencao anterior = unidade.getStatusManutencao();
        unidade.setStatusManutencao(status);
        if (status == UnidadeInventario.StatusManutencao.ABERTA
                || status == UnidadeInventario.StatusManutencao.EM_EXECUCAO
                || status == UnidadeInventario.StatusManutencao.SUSPENSA) {
            alterarCondicao(unidade, UnidadeInventario.CondicaoEquipamento.EM_REPARO,
                    "MANUTENCAO", request.fornecedor());
        } else if (status == UnidadeInventario.StatusManutencao.CONCLUIDA) {
            alterarCondicao(unidade, UnidadeInventario.CondicaoEquipamento.OPERACIONAL,
                    "MANUTENCAO", request.fornecedor());
        }
        registrarHistorico(unidade, "STATUS_MANUTENCAO", anterior.name(), status.name(),
                "INVENTARIO", request.fornecedor());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.UnidadeDetalheResposta registrarReefer(
            Long unidadeId,
            InventarioCanonicoDTO.ReeferRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Dados reefer são obrigatórios");
        }
        UnidadeInventario unidade = localizarUnidade(unidadeId);
        if (!unidade.getTipoEquipamento().isRefrigerado()) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY, "Tipo de equipamento não possui controle reefer");
        }
        validarPercentual(request.umidadePercentual(), "Umidade");
        validarNaoNegativo(request.ventilacaoM3h(), "Ventilação");
        LocalDateTime leitura = request.lidoEm() == null ? LocalDateTime.now() : request.lidoEm();
        unidade.getRegistrosReefer().add(new UnidadeInventario.ReeferRegistro(
                request.setpointC(), request.temperaturaSupplyC(), request.temperaturaReturnC(),
                request.umidadePercentual(), request.ventilacaoM3h(), request.ligado(),
                normalizarOpcional(request.alarme()), leitura, normalizarOpcional(request.responsavel())));
        registrarHistorico(unidade, "REEFER", null,
                "setpoint=" + Objects.toString(request.setpointC(), "-")
                        + ";supply=" + Objects.toString(request.temperaturaSupplyC(), "-")
                        + ";return=" + Objects.toString(request.temperaturaReturnC(), "-"),
                "REEFER", request.responsavel());
        unidadeRepositorio.save(unidade);
        return detalhar(unidadeId);
    }

    @Transactional
    public InventarioCanonicoDTO.VinculoResposta montar(InventarioCanonicoDTO.MontagemRequest request) {
        if (request == null || request.unidadePrincipalId() == null || request.unidadeRelacionadaId() == null
                || request.papel() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Unidades e papel do vínculo são obrigatórios");
        }
        if (Objects.equals(request.unidadePrincipalId(), request.unidadeRelacionadaId())) {
            throw erro(HttpStatus.BAD_REQUEST, "Uma unidade não pode ser montada nela mesma");
        }
        UnidadeInventario principal = localizarUnidade(request.unidadePrincipalId());
        UnidadeInventario relacionada = localizarUnidade(request.unidadeRelacionadaId());
        boolean vinculoDuplicado = vinculoRepositorio.existsByUnidadePrincipalIdAndUnidadeRelacionadaIdAndAtivoTrue(
                principal.getId(), relacionada.getId())
                || vinculoRepositorio.existsByUnidadePrincipalIdAndUnidadeRelacionadaIdAndAtivoTrue(
                relacionada.getId(), principal.getId());
        if (vinculoDuplicado) {
            throw erro(HttpStatus.CONFLICT, "Equipamentos já possuem vínculo ativo");
        }
        validarPapelMontagem(principal, relacionada, request.papel());
        VinculoEquipamento vinculo = new VinculoEquipamento();
        vinculo.setUnidadePrincipal(principal);
        vinculo.setUnidadeRelacionada(relacionada);
        vinculo.setPapel(request.papel());
        vinculo.setAtivo(true);
        vinculo.setMontadoEm(LocalDateTime.now());
        vinculo.setResponsavelMontagem(normalizarOpcional(request.responsavel()));
        vinculo.setObservacoes(normalizarOpcional(request.observacoes()));
        VinculoEquipamento salvo = vinculoRepositorio.save(vinculo);
        registrarHistorico(principal, "MONTAGEM", null, relacionada.getIdentificacao(),
                "INVENTARIO", request.responsavel());
        registrarHistorico(relacionada, "MONTAGEM_EM", null, principal.getIdentificacao(),
                "INVENTARIO", request.responsavel());
        unidadeRepositorio.save(principal);
        unidadeRepositorio.save(relacionada);
        return mapearVinculo(salvo);
    }

    @Transactional
    public InventarioCanonicoDTO.VinculoResposta desmontar(Long vinculoId,
                                                            InventarioCanonicoDTO.DesmontagemRequest request) {
        VinculoEquipamento vinculo = vinculoRepositorio.findByIdAndAtivoTrue(vinculoId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Vínculo ativo não encontrado"));
        String responsavel = request == null ? null : normalizarOpcional(request.responsavel());
        String motivo = request == null ? null : normalizarOpcional(request.motivo());
        vinculo.desmontar(responsavel, LocalDateTime.now());
        if (motivo != null) {
            vinculo.setObservacoes(motivo);
        }
        registrarHistorico(vinculo.getUnidadePrincipal(), "DESMONTAGEM",
                vinculo.getUnidadeRelacionada().getIdentificacao(), null, "INVENTARIO", responsavel);
        registrarHistorico(vinculo.getUnidadeRelacionada(), "DESMONTAGEM_DE",
                vinculo.getUnidadePrincipal().getIdentificacao(), null, "INVENTARIO", responsavel);
        unidadeRepositorio.save(vinculo.getUnidadePrincipal());
        unidadeRepositorio.save(vinculo.getUnidadeRelacionada());
        return mapearVinculo(vinculoRepositorio.save(vinculo));
    }

    @Transactional
    public InventarioCanonicoDTO.ContagemLoteResposta registrarContagem(
            InventarioCanonicoDTO.ContagemLoteRequest request) {
        if (request == null || request.itens() == null || request.itens().isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "A contagem deve possuir ao menos um item");
        }
        String lote = obrigatorio(request.lote(), "Lote da contagem é obrigatório").toUpperCase(Locale.ROOT);
        String responsavel = normalizarOpcional(request.responsavel());
        List<ContagemInventarioFisico> registros = new ArrayList<>();

        for (InventarioCanonicoDTO.ItemContagemRequest item : request.itens()) {
            if (item == null) {
                continue;
            }
            String identificacao = normalizarIdentificacao(item.identificacao());
            Optional<UnidadeInventario> unidade = unidadeRepositorio.findByIdentificacaoIgnoreCase(identificacao);
            ContagemInventarioFisico contagem = new ContagemInventarioFisico();
            contagem.setLote(lote);
            contagem.setUnidade(unidade.orElse(null));
            contagem.setIdentificacaoLida(identificacao);
            contagem.setPosicaoEsperada(unidade.map(UnidadeInventario::getPosicaoAtual).orElse(null));
            contagem.setPosicaoLida(normalizarOpcional(item.posicaoLida()));
            contagem.setObservacoes(normalizarOpcional(item.observacoes()));
            contagem.setResponsavel(responsavel);
            contagem.setRegistradoEm(LocalDateTime.now());
            classificarContagem(contagem, unidade, item.encontrada());
            registros.add(contagemRepositorio.save(contagem));
        }

        List<InventarioCanonicoDTO.DivergenciaResposta> respostas = registros.stream()
                .map(this::mapearDivergencia)
                .collect(Collectors.toList());
        long conferentes = registros.stream()
                .filter(registro -> registro.getStatus() == ContagemInventarioFisico.StatusContagem.CONFERENTE)
                .count();
        return new InventarioCanonicoDTO.ContagemLoteResposta(
                lote,
                registros.size(),
                conferentes,
                registros.size() - conferentes,
                respostas);
    }

    @Transactional(readOnly = true)
    public List<InventarioCanonicoDTO.DivergenciaResposta> listarDivergencias() {
        return contagemRepositorio.findByStatusNotOrderByRegistradoEmDesc(
                        ContagemInventarioFisico.StatusContagem.RESOLVIDA)
                .stream()
                .map(this::mapearDivergencia)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventarioCanonicoDTO.DivergenciaResposta resolverDivergencia(
            Long divergenciaId,
            InventarioCanonicoDTO.ResolverDivergenciaRequest request) {
        ContagemInventarioFisico divergencia = contagemRepositorio.findById(divergenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Divergência não encontrada"));
        if (divergencia.getStatus() == ContagemInventarioFisico.StatusContagem.RESOLVIDA) {
            return mapearDivergencia(divergencia);
        }
        String responsavel = request == null
                ? null
                : obrigatorio(request.responsavel(), "Responsável pela resolução é obrigatório");
        divergencia.resolver(responsavel);
        if (request != null && StringUtils.hasText(request.observacoes())) {
            divergencia.setObservacoes(normalizarOpcional(request.observacoes()));
        }
        return mapearDivergencia(contagemRepositorio.save(divergencia));
    }

    private void sincronizarConteinersLegados() {
        TipoEquipamentoInventario tipoPadrao = obterOuCriarTipoConteinerPadrao();
        for (ConteinerPatio conteiner : conteinerPatioRepositorio.findAllByOrderByCodigoAsc()) {
            if (unidadeRepositorio.existsByIdentificacaoIgnoreCase(conteiner.getCodigo())) {
                continue;
            }
            UnidadeInventario unidade = new UnidadeInventario();
            unidade.setIdentificacao(conteiner.getCodigo().toUpperCase(Locale.ROOT));
            unidade.setPrefixo(extrairPrefixo(conteiner.getCodigo()));
            unidade.setTipoEquipamento(tipoPadrao);
            unidade.setCategoria(TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
            unidade.setEstado(mapearEstadoLegado(conteiner.getStatus()));
            unidade.setCondicao(conteiner.getStatus() == StatusConteiner.DANIFICADO
                    ? UnidadeInventario.CondicaoEquipamento.AVARIADO
                    : UnidadeInventario.CondicaoEquipamento.OPERACIONAL);
            unidade.setStatusManutencao(UnidadeInventario.StatusManutencao.NAO_REQUERIDA);
            unidade.setPosicaoAtual(formatarPosicao(conteiner.getPosicao()));
            unidade.setPosicaoPlanejada(formatarPosicao(conteiner.getPosicao()));
            unidade.setPesoBrutoKg(conteiner.getPesoToneladas() == null
                    ? null
                    : conteiner.getPesoToneladas().multiply(new BigDecimal("1000")));
            unidade.setObservacoes(normalizarOpcional(conteiner.getRestricoes()));
            registrarHistorico(unidade, "IMPORTACAO_LEGADO", null, conteiner.getStatus().name(),
                    "SERVICO_YARD", "sincronizacao-automatica");
            UnidadeInventario salva = unidadeRepositorio.save(unidade);
            garantirPrefixoCadastrado(salva);
        }
    }

    private TipoEquipamentoInventario obterOuCriarTipoConteinerPadrao() {
        return tipoRepositorio.findByCodigoIgnoreCase(TIPO_CONTEINER_PADRAO)
                .orElseGet(() -> {
                    TipoEquipamentoInventario tipo = new TipoEquipamentoInventario();
                    tipo.setCodigo(TIPO_CONTEINER_PADRAO);
                    tipo.setDescricao("Contêiner sem tipo ISO informado");
                    tipo.setCategoria(TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER);
                    tipo.setRefrigerado(false);
                    tipo.setAtivo(true);
                    return tipoRepositorio.save(tipo);
                });
    }

    private InventarioCanonicoDTO.ResumoInventario criarResumo(
            List<InventarioCanonicoDTO.UnidadeResumo> unidades,
            long divergenciasAbertas) {
        LocalDateTime atualizadoEm = unidades.stream()
                .map(InventarioCanonicoDTO.UnidadeResumo::atualizadoEm)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new InventarioCanonicoDTO.ResumoInventario(
                unidades.size(),
                contarCategoria(unidades, TipoEquipamentoInventario.CategoriaEquipamento.CONTEINER),
                contarCategoria(unidades, TipoEquipamentoInventario.CategoriaEquipamento.CHASSI),
                contarCategoria(unidades, TipoEquipamentoInventario.CategoriaEquipamento.CARRETA),
                contarCategoria(unidades, TipoEquipamentoInventario.CategoriaEquipamento.ACESSORIO),
                unidades.stream().filter(unidade -> unidade.estado() == UnidadeInventario.EstadoUnidade.NO_PATIO).count(),
                unidades.stream().filter(unidade -> unidade.holdsAtivos() > 0).count(),
                unidades.stream().filter(unidade -> unidade.condicao() == UnidadeInventario.CondicaoEquipamento.AVARIADO
                        || unidade.condicao() == UnidadeInventario.CondicaoEquipamento.INOPERANTE).count(),
                unidades.stream().filter(unidade -> unidade.statusManutencao() != UnidadeInventario.StatusManutencao.NAO_REQUERIDA
                        && unidade.statusManutencao() != UnidadeInventario.StatusManutencao.CONCLUIDA).count(),
                unidades.stream().filter(InventarioCanonicoDTO.UnidadeResumo::refrigerado).count(),
                unidades.stream().filter(unidade -> unidade.equipamentosVinculados() > 0).count(),
                divergenciasAbertas,
                atualizadoEm);
    }

    private long contarCategoria(List<InventarioCanonicoDTO.UnidadeResumo> unidades,
                                 TipoEquipamentoInventario.CategoriaEquipamento categoria) {
        return unidades.stream().filter(unidade -> unidade.categoria() == categoria).count();
    }

    private InventarioCanonicoDTO.UnidadeResumo mapearResumo(UnidadeInventario unidade) {
        LocalDateTime agora = LocalDateTime.now();
        long holdsAtivos = unidade.getRestricoes().stream()
                .filter(restricao -> restricao.getTipo() == UnidadeInventario.TipoRestricao.HOLD)
                .filter(restricao -> restricao.isAtiva()
                        && (restricao.getValidoDe() == null || !restricao.getValidoDe().isAfter(agora))
                        && (restricao.getValidoAte() == null || !restricao.getValidoAte().isBefore(agora)))
                .count();
        long permissionsAtivas = unidade.getRestricoes().stream()
                .filter(restricao -> restricao.getTipo() == UnidadeInventario.TipoRestricao.PERMISSION)
                .filter(restricao -> restricao.isAtiva()
                        && (restricao.getValidoDe() == null || !restricao.getValidoDe().isAfter(agora))
                        && (restricao.getValidoAte() == null || !restricao.getValidoAte().isBefore(agora)))
                .count();
        long avariasAbertas = unidade.getAvarias().stream()
                .filter(avaria -> avaria.getStatus() == UnidadeInventario.StatusAvaria.ABERTA
                        || avaria.getStatus() == UnidadeInventario.StatusAvaria.EM_REPARO)
                .count();
        long vinculosAtivos = vinculoRepositorio
                .findByUnidadePrincipalIdOrUnidadeRelacionadaIdOrderByMontadoEmDesc(unidade.getId(), unidade.getId())
                .stream()
                .filter(VinculoEquipamento::isAtivo)
                .count();
        TipoEquipamentoInventario tipo = unidade.getTipoEquipamento();
        return new InventarioCanonicoDTO.UnidadeResumo(
                unidade.getId(),
                unidade.getIdentificacao(),
                unidade.getPrefixo(),
                unidade.getCategoria(),
                tipo.getCodigo(),
                tipo.getDescricao(),
                tipo.getCodigoIso(),
                unidade.getEstado(),
                unidade.getCondicao(),
                unidade.getStatusManutencao(),
                unidade.getProprietario(),
                unidade.getOperador(),
                unidade.getPosicaoAtual(),
                unidade.getPosicaoPlanejada(),
                unidade.getPesoBrutoKg(),
                tipo.isRefrigerado(),
                holdsAtivos,
                permissionsAtivas,
                avariasAbertas,
                unidade.getDocumentos().size(),
                vinculosAtivos,
                unidade.getAtualizadoEm());
    }

    private InventarioCanonicoDTO.TipoEquipamentoResposta mapearTipo(TipoEquipamentoInventario tipo) {
        return new InventarioCanonicoDTO.TipoEquipamentoResposta(
                tipo.getId(), tipo.getCodigo(), tipo.getDescricao(), tipo.getCategoria(), tipo.getCodigoIso(),
                tipo.getComprimentoMm(), tipo.getLarguraMm(), tipo.getAlturaMm(), tipo.getTaraKg(),
                tipo.getCapacidadeKg(), tipo.isRefrigerado(), tipo.getGrupoEquivalencia(), tipo.isAtivo());
    }

    private InventarioCanonicoDTO.PrefixoResposta mapearPrefixo(PrefixoEquipamentoInventario prefixo) {
        return new InventarioCanonicoDTO.PrefixoResposta(
                prefixo.getId(), prefixo.getPrefixo(), prefixo.getProprietario(),
                prefixo.getCategoria(), prefixo.isAtivo());
    }

    private InventarioCanonicoDTO.VinculoResposta mapearVinculo(VinculoEquipamento vinculo) {
        return new InventarioCanonicoDTO.VinculoResposta(
                vinculo.getId(),
                vinculo.getUnidadePrincipal().getId(),
                vinculo.getUnidadePrincipal().getIdentificacao(),
                vinculo.getUnidadeRelacionada().getId(),
                vinculo.getUnidadeRelacionada().getIdentificacao(),
                vinculo.getPapel(),
                vinculo.isAtivo(),
                vinculo.getMontadoEm(),
                vinculo.getDesmontadoEm(),
                vinculo.getResponsavelMontagem(),
                vinculo.getResponsavelDesmontagem(),
                vinculo.getObservacoes());
    }

    private InventarioCanonicoDTO.DivergenciaResposta mapearDivergencia(ContagemInventarioFisico divergencia) {
        return new InventarioCanonicoDTO.DivergenciaResposta(
                divergencia.getId(),
                divergencia.getLote(),
                divergencia.getUnidade() == null ? null : divergencia.getUnidade().getId(),
                divergencia.getIdentificacaoLida(),
                divergencia.getPosicaoEsperada(),
                divergencia.getPosicaoLida(),
                divergencia.getStatus(),
                divergencia.getTipoDivergencia(),
                divergencia.getObservacoes(),
                divergencia.getResponsavel(),
                divergencia.getRegistradoEm(),
                divergencia.getResolvidoEm(),
                divergencia.getResolvidoPor());
    }

    private void classificarContagem(ContagemInventarioFisico contagem,
                                     Optional<UnidadeInventario> unidade,
                                     boolean encontrada) {
        if (!encontrada && unidade.isPresent()) {
            contagem.setStatus(ContagemInventarioFisico.StatusContagem.NAO_LOCALIZADA);
            contagem.setTipoDivergencia(ContagemInventarioFisico.TipoDivergencia.UNIDADE_NAO_LOCALIZADA);
            return;
        }
        if (encontrada && unidade.isEmpty()) {
            contagem.setStatus(ContagemInventarioFisico.StatusContagem.NAO_PREVISTA);
            contagem.setTipoDivergencia(ContagemInventarioFisico.TipoDivergencia.UNIDADE_NAO_PREVISTA);
            return;
        }
        if (unidade.isEmpty()) {
            contagem.setStatus(ContagemInventarioFisico.StatusContagem.NAO_PREVISTA);
            contagem.setTipoDivergencia(ContagemInventarioFisico.TipoDivergencia.UNIDADE_NAO_PREVISTA);
            return;
        }
        String esperada = normalizarComparacao(contagem.getPosicaoEsperada());
        String lida = normalizarComparacao(contagem.getPosicaoLida());
        if (Objects.equals(esperada, lida)) {
            contagem.setStatus(ContagemInventarioFisico.StatusContagem.CONFERENTE);
            contagem.setTipoDivergencia(null);
        } else {
            contagem.setStatus(ContagemInventarioFisico.StatusContagem.DIVERGENTE);
            contagem.setTipoDivergencia(ContagemInventarioFisico.TipoDivergencia.POSICAO);
        }
    }

    private void validarPapelMontagem(UnidadeInventario principal,
                                      UnidadeInventario relacionada,
                                      VinculoEquipamento.PapelEquipamento papel) {
        if (papel == VinculoEquipamento.PapelEquipamento.TRANSPORTE
                && relacionada.getCategoria() != TipoEquipamentoInventario.CategoriaEquipamento.CHASSI
                && relacionada.getCategoria() != TipoEquipamentoInventario.CategoriaEquipamento.CARRETA) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Papel TRANSPORTE exige chassi ou carreta como unidade relacionada");
        }
        if ((papel == VinculoEquipamento.PapelEquipamento.ACESSORIO
                || papel == VinculoEquipamento.PapelEquipamento.ACESSORIO_NO_CHASSI)
                && relacionada.getCategoria() != TipoEquipamentoInventario.CategoriaEquipamento.ACESSORIO) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Papel de acessório exige unidade relacionada da categoria ACESSORIO");
        }
        if (principal.getEstado() == UnidadeInventario.EstadoUnidade.APOSENTADA
                || relacionada.getEstado() == UnidadeInventario.EstadoUnidade.APOSENTADA) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY, "Equipamento aposentado não pode ser montado");
        }
    }

    private void alterarCondicao(UnidadeInventario unidade,
                                 UnidadeInventario.CondicaoEquipamento novaCondicao,
                                 String origem,
                                 String responsavel) {
        UnidadeInventario.CondicaoEquipamento anterior = unidade.getCondicao();
        unidade.setCondicao(novaCondicao);
        registrarHistorico(unidade, "CONDICAO", anterior == null ? null : anterior.name(),
                novaCondicao.name(), origem, responsavel);
    }

    private void registrarHistorico(UnidadeInventario unidade,
                                    String atributo,
                                    Object anterior,
                                    Object atual,
                                    String origem,
                                    String responsavel) {
        unidade.getHistoricoAtributos().add(new UnidadeInventario.HistoricoAtributoRegistro(
                atributo,
                Objects.toString(anterior, null),
                Objects.toString(atual, null),
                normalizarOpcional(origem),
                normalizarOpcional(responsavel),
                LocalDateTime.now()));
    }

    private UnidadeInventario localizarUnidade(Long unidadeId) {
        if (unidadeId == null || unidadeId <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Identificador da unidade é inválido");
        }
        return unidadeRepositorio.findById(unidadeId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Unidade não encontrada"));
    }

    private TipoEquipamentoInventario localizarTipo(String codigo) {
        String codigoNormalizado = obrigatorio(codigo, "Tipo de equipamento é obrigatório")
                .toUpperCase(Locale.ROOT);
        return tipoRepositorio.findByCodigoIgnoreCase(codigoNormalizado)
                .filter(TipoEquipamentoInventario::isAtivo)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Tipo de equipamento não encontrado ou inativo"));
    }

    private void garantirPrefixoCadastrado(UnidadeInventario unidade) {
        if (!StringUtils.hasText(unidade.getPrefixo())
                || prefixoRepositorio.existsByPrefixoIgnoreCase(unidade.getPrefixo())) {
            return;
        }
        PrefixoEquipamentoInventario prefixo = new PrefixoEquipamentoInventario();
        prefixo.setPrefixo(unidade.getPrefixo());
        prefixo.setProprietario(unidade.getProprietario());
        prefixo.setCategoria(unidade.getCategoria());
        prefixo.setAtivo(true);
        prefixoRepositorio.save(prefixo);
    }

    private UnidadeInventario.EstadoUnidade mapearEstadoLegado(StatusConteiner status) {
        if (status == StatusConteiner.LIBERADO) {
            return UnidadeInventario.EstadoUnidade.LIBERADA;
        }
        if (status == StatusConteiner.DESPACHADO) {
            return UnidadeInventario.EstadoUnidade.DESPACHADA;
        }
        return UnidadeInventario.EstadoUnidade.NO_PATIO;
    }

    private String formatarPosicao(PosicaoPatio posicao) {
        if (posicao == null) {
            return null;
        }
        return posicao.getLinha() + "-" + posicao.getColuna() + "-" + posicao.getCamadaOperacional();
    }

    private String extrairPrefixo(String identificacao) {
        String normalizada = normalizarIdentificacao(identificacao);
        StringBuilder prefixo = new StringBuilder();
        for (int indice = 0; indice < normalizada.length() && prefixo.length() < 4; indice++) {
            char caractere = normalizada.charAt(indice);
            if (!Character.isLetter(caractere)) {
                break;
            }
            prefixo.append(caractere);
        }
        return prefixo.length() >= 2 ? prefixo.toString() : null;
    }

    private String normalizarIdentificacao(String valor) {
        String limpo = obrigatorio(valor, "Identificação da unidade é obrigatória")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9-]", "");
        if (limpo.length() < 3 || limpo.length() > 40) {
            throw erro(HttpStatus.BAD_REQUEST, "Identificação deve possuir entre 3 e 40 caracteres");
        }
        return limpo;
    }

    private String obrigatorio(String valor, String mensagem) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        if (!StringUtils.hasText(limpo)) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
        return limpo;
    }

    private String normalizarOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo : null;
    }

    private String normalizarComparacao(String valor) {
        String normalizado = normalizarOpcional(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT).replace(" ", "");
    }

    private boolean contemIgnorandoCaixa(String valor, String filtroNormalizado) {
        return filtroNormalizado == null
                || (valor != null && valor.toUpperCase(Locale.ROOT).contains(filtroNormalizado));
    }

    private String combinarResponsavelMotivo(String responsavel, String motivo) {
        String responsavelLimpo = normalizarOpcional(responsavel);
        String motivoLimpo = normalizarOpcional(motivo);
        if (motivoLimpo == null) {
            return responsavelLimpo;
        }
        return responsavelLimpo == null ? motivoLimpo : responsavelLimpo + " - " + motivoLimpo;
    }

    private void validarDimensao(Integer valor, String campo) {
        if (valor != null && valor <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, campo + " deve ser maior que zero");
        }
    }

    private void validarNaoNegativo(BigDecimal valor, String campo) {
        if (valor != null && valor.signum() < 0) {
            throw erro(HttpStatus.BAD_REQUEST, campo + " não pode ser negativo");
        }
    }

    private void validarPercentual(BigDecimal valor, String campo) {
        if (valor != null && (valor.signum() < 0 || valor.compareTo(new BigDecimal("100")) > 0)) {
            throw erro(HttpStatus.BAD_REQUEST, campo + " deve estar entre 0 e 100");
        }
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
