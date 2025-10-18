package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.patio.dto.ConteinerMapaDto;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoMapaDto;
import br.com.cloudport.servicoyard.patio.dto.EquipamentoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.dto.EventoMapaTempoRealDto;
import br.com.cloudport.servicoyard.patio.dto.FiltrosMapaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioFiltro;
import br.com.cloudport.servicoyard.patio.dto.MapaPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.dto.MovimentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.OpcoesCadastroPatioDto;
import br.com.cloudport.servicoyard.patio.dto.PosicaoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.EventoMovimentoPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.CargaPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.CargaPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
public class MapaPatioServico {

    private static final String TOPICO_ATUALIZACOES = "/topico/patio";

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final PosicaoPatioRepositorio posicaoPatioRepositorio;
    private final CargaPatioRepositorio cargaPatioRepositorio;
    private final MovimentoPatioRepositorio movimentoPatioRepositorio;
    private final SimpMessagingTemplate messagingTemplate;
    private final PublicadorEventoMovimentoPatio publicadorEventoMovimentoPatio;

    public MapaPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                             EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                             PosicaoPatioRepositorio posicaoPatioRepositorio,
                             CargaPatioRepositorio cargaPatioRepositorio,
                             MovimentoPatioRepositorio movimentoPatioRepositorio,
                             SimpMessagingTemplate messagingTemplate,
                             PublicadorEventoMovimentoPatio publicadorEventoMovimentoPatio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.posicaoPatioRepositorio = posicaoPatioRepositorio;
        this.cargaPatioRepositorio = cargaPatioRepositorio;
        this.movimentoPatioRepositorio = movimentoPatioRepositorio;
        this.messagingTemplate = messagingTemplate;
        this.publicadorEventoMovimentoPatio = publicadorEventoMovimentoPatio;
    }

    @Transactional(readOnly = true)
    public MapaPatioRespostaDto consultarMapa(MapaPatioFiltro filtro) {
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAllByOrderByTipoEquipamentoAscIdentificadorAsc();

        List<ConteinerMapaDto> conteineresFiltrados = conteineres.stream()
                .filter(conteiner -> filtrarConteiner(conteiner, filtro))
                .map(this::converterConteiner)
                .collect(Collectors.toList());

        List<EquipamentoMapaDto> equipamentosFiltrados = equipamentos.stream()
                .filter(equipamento -> filtrarEquipamento(equipamento, filtro))
                .map(this::converterEquipamento)
                .collect(Collectors.toList());

        int totalLinhas = conteineres.stream()
                .map(conteiner -> conteiner.getPosicao().getLinha())
                .max(Integer::compareTo)
                .orElse(0);
        int totalColunas = conteineres.stream()
                .map(conteiner -> conteiner.getPosicao().getColuna())
                .max(Integer::compareTo)
                .orElse(0);
        LocalDateTime ultimaAtualizacao = conteineres.stream()
                .map(ConteinerPatio::getAtualizadoEm)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return new MapaPatioRespostaDto(conteineresFiltrados, equipamentosFiltrados,
                totalLinhas + 1, totalColunas + 1, ultimaAtualizacao);
    }

    @Transactional(readOnly = true)
    public FiltrosMapaPatioDto consultarFiltros() {
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAll();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAll();

        Set<String> status = conteineres.stream()
                .map(conteiner -> conteiner.getStatus().name())
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> tiposCarga = conteineres.stream()
                .map(conteiner -> escapar(conteiner.getCarga().getCodigo()))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> destinos = conteineres.stream()
                .map(conteiner -> escapar(conteiner.getDestino()))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> camadas = conteineres.stream()
                .map(conteiner -> escapar(conteiner.getPosicao().getCamadaOperacional()))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> tiposEquipamento = equipamentos.stream()
                .map(equipamento -> equipamento.getTipoEquipamento().name())
                .collect(Collectors.toCollection(TreeSet::new));

        return new FiltrosMapaPatioDto(
                List.copyOf(status),
                List.copyOf(tiposCarga),
                List.copyOf(destinos),
                List.copyOf(camadas),
                List.copyOf(tiposEquipamento)
        );
    }

    @Transactional(readOnly = true)
    public List<PosicaoPatioDto> listarPosicoes() {
        List<PosicaoPatio> posicoes = posicaoPatioRepositorio.findAll(
                Sort.by(Sort.Order.asc("linha"), Sort.Order.asc("coluna"), Sort.Order.asc("camadaOperacional"))
        );
        Map<Long, ConteinerPatio> conteineresPorPosicao = conteinerPatioRepositorio.findAll().stream()
                .filter(conteiner -> conteiner.getPosicao() != null && conteiner.getPosicao().getId() != null)
                .collect(Collectors.toMap(
                        conteiner -> conteiner.getPosicao().getId(),
                        Function.identity(),
                        (conteinerExistente, novoConteiner) -> conteinerExistente
                ));

        return posicoes.stream()
                .map(posicao -> converterPosicao(posicao, conteineresPorPosicao.get(posicao.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConteinerMapaDto> listarConteineres() {
        return conteinerPatioRepositorio.findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc().stream()
                .map(this::converterConteiner)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimentoPatioDto> listarMovimentacoesRecentes() {
        return movimentoPatioRepositorio.findTop50ByOrderByRegistradoEmDesc().stream()
                .map(this::converterMovimento)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OpcoesCadastroPatioDto consultarOpcoesCadastro() {
        List<String> statusConteiner = Arrays.stream(StatusConteiner.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        List<String> tiposEquipamento = Arrays.stream(TipoEquipamento.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        List<String> statusEquipamento = Arrays.stream(StatusEquipamento.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return new OpcoesCadastroPatioDto(statusConteiner, tiposEquipamento, statusEquipamento);
    }

    @Transactional
    public ConteinerMapaDto registrarOuAtualizarConteiner(ConteinerPatioRequisicaoDto requisicaoDto) {
        ConteinerPatio conteiner = localizarConteiner(requisicaoDto);
        boolean novoConteiner = conteiner.getId() == null;

        PosicaoPatio posicao = obterOuCriarPosicao(requisicaoDto);
        CargaPatio carga = obterOuCriarCarga(requisicaoDto.getTipoCarga());

        conteiner.setCodigo(requisicaoDto.getCodigo());
        conteiner.setPosicao(posicao);
        conteiner.setStatus(requisicaoDto.getStatus());
        conteiner.setCarga(carga);
        conteiner.setDestino(requisicaoDto.getDestino());
        conteiner.setAtualizadoEm(requisicaoDto.gerarHorarioAtualizacao());

        ConteinerPatio salvo = conteinerPatioRepositorio.save(conteiner);
        registrarMovimento(salvo, novoConteiner ? TipoMovimentoPatio.ALOCACAO : TipoMovimentoPatio.ATUALIZACAO);
        publicarAtualizacaoTempoReal();
        return converterConteiner(salvo);
    }

    @Transactional
    public EquipamentoMapaDto registrarOuAtualizarEquipamento(EquipamentoPatioRequisicaoDto requisicaoDto) {
        EquipamentoPatio equipamento = localizarEquipamento(requisicaoDto);
        equipamento.setIdentificador(requisicaoDto.getIdentificador());
        equipamento.setTipoEquipamento(requisicaoDto.getTipoEquipamento());
        equipamento.setLinha(requisicaoDto.getLinha());
        equipamento.setColuna(requisicaoDto.getColuna());
        equipamento.setStatusOperacional(requisicaoDto.getStatusOperacional());

        EquipamentoPatio salvo = equipamentoPatioRepositorio.save(equipamento);
        publicarAtualizacaoTempoReal();
        return converterEquipamento(salvo);
    }

    public MapaPatioFiltro construirFiltro(List<String> status, List<String> tiposCarga, List<String> destinos,
                                           List<String> camadas, List<String> tiposEquipamento) {
        return new MapaPatioFiltro(
                normalizarLista(status),
                normalizarLista(tiposCarga),
                normalizarLista(destinos),
                normalizarLista(camadas),
                normalizarLista(tiposEquipamento)
        );
    }

    private PosicaoPatioDto converterPosicao(PosicaoPatio posicao, ConteinerPatio conteiner) {
        boolean ocupada = conteiner != null;
        String codigoConteiner = ocupada ? escapar(conteiner.getCodigo()) : null;
        StatusConteiner statusConteiner = ocupada ? conteiner.getStatus() : null;
        return new PosicaoPatioDto(
                posicao.getId(),
                posicao.getLinha(),
                posicao.getColuna(),
                escapar(posicao.getCamadaOperacional()),
                ocupada,
                codigoConteiner,
                statusConteiner
        );
    }

    private MovimentoPatioDto converterMovimento(MovimentoPatio movimento) {
        ConteinerPatio conteiner = movimento.getConteiner();
        Integer linha = null;
        Integer coluna = null;
        String camada = null;
        String destino = null;
        String codigo = null;
        if (conteiner != null && conteiner.getPosicao() != null) {
            linha = conteiner.getPosicao().getLinha();
            coluna = conteiner.getPosicao().getColuna();
            camada = escapar(conteiner.getPosicao().getCamadaOperacional());
        }
        if (conteiner != null) {
            destino = escapar(conteiner.getDestino());
            codigo = escapar(conteiner.getCodigo());
        }
        return new MovimentoPatioDto(
                movimento.getId(),
                codigo,
                movimento.getTipoMovimento(),
                escapar(movimento.getDescricao()),
                destino,
                linha,
                coluna,
                camada,
                movimento.getRegistradoEm()
        );
    }

    private List<String> normalizarLista(List<String> valores) {
        return ValidacaoEntradaUtil.limparLista(valores).stream()
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private boolean filtrarConteiner(ConteinerPatio conteiner, MapaPatioFiltro filtro) {
        return verificarFiltro(filtro.getStatus(), conteiner.getStatus().name())
                && verificarFiltro(filtro.getTiposCarga(), conteiner.getCarga().getCodigo())
                && verificarFiltro(filtro.getDestinos(), conteiner.getDestino())
                && verificarFiltro(filtro.getCamadasOperacionais(), conteiner.getPosicao().getCamadaOperacional());
    }

    private boolean filtrarEquipamento(EquipamentoPatio equipamento, MapaPatioFiltro filtro) {
        return verificarFiltro(filtro.getTiposEquipamento(), equipamento.getTipoEquipamento().name());
    }

    private boolean verificarFiltro(List<String> filtros, String valor) {
        if (filtros == null || filtros.isEmpty()) {
            return true;
        }
        return filtros.contains(valor.toUpperCase(Locale.ROOT));
    }

    private ConteinerPatio localizarConteiner(ConteinerPatioRequisicaoDto requisicaoDto) {
        if (requisicaoDto.getId() != null) {
            return conteinerPatioRepositorio.findById(requisicaoDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Contêiner não encontrado."));
        }
        Optional<ConteinerPatio> existente = conteinerPatioRepositorio.findByCodigo(requisicaoDto.getCodigo());
        return existente.orElseGet(ConteinerPatio::new);
    }

    private PosicaoPatio obterOuCriarPosicao(ConteinerPatioRequisicaoDto requisicaoDto) {
        return posicaoPatioRepositorio
                .findByLinhaAndColunaAndCamadaOperacional(
                        requisicaoDto.getLinha(),
                        requisicaoDto.getColuna(),
                        requisicaoDto.getCamadaOperacional())
                .orElseGet(() -> {
                    PosicaoPatio novaPosicao = new PosicaoPatio();
                    novaPosicao.setLinha(requisicaoDto.getLinha());
                    novaPosicao.setColuna(requisicaoDto.getColuna());
                    novaPosicao.setCamadaOperacional(requisicaoDto.getCamadaOperacional());
                    return posicaoPatioRepositorio.save(novaPosicao);
                });
    }

    private CargaPatio obterOuCriarCarga(String codigoCarga) {
        return cargaPatioRepositorio.findByCodigo(codigoCarga)
                .orElseGet(() -> {
                    CargaPatio novaCarga = new CargaPatio();
                    novaCarga.setCodigo(codigoCarga);
                    novaCarga.setDescricao(codigoCarga);
                    return cargaPatioRepositorio.save(novaCarga);
                });
    }

    private void registrarMovimento(ConteinerPatio conteiner, TipoMovimentoPatio tipoMovimento) {
        MovimentoPatio movimento = new MovimentoPatio();
        movimento.setConteiner(conteiner);
        movimento.setTipoMovimento(tipoMovimento);
        movimento.setDescricao(gerarDescricaoMovimento(conteiner, tipoMovimento));
        movimento.setRegistradoEm(LocalDateTime.now());
        MovimentoPatio registrado = movimentoPatioRepositorio.save(movimento);
        publicadorEventoMovimentoPatio.publicar(criarEventoMovimento(conteiner, registrado));
    }

    private String gerarDescricaoMovimento(ConteinerPatio conteiner, TipoMovimentoPatio tipoMovimento) {
        String base = tipoMovimento == TipoMovimentoPatio.ALOCACAO ? "Alocação" : "Atualização";
        return String.format(Locale.ROOT,
                "%s do contêiner %s para a posição (%d,%d) - camada %s",
                base,
                conteiner.getCodigo(),
                conteiner.getPosicao().getLinha(),
                conteiner.getPosicao().getColuna(),
                conteiner.getPosicao().getCamadaOperacional());
    }

    private EventoMovimentoPatioDto criarEventoMovimento(ConteinerPatio conteiner, MovimentoPatio movimento) {
        return new EventoMovimentoPatioDto(
                escapar(conteiner.getCodigo()),
                movimento.getTipoMovimento().name(),
                escapar(movimento.getDescricao()),
                escapar(conteiner.getDestino()),
                conteiner.getPosicao().getLinha(),
                conteiner.getPosicao().getColuna(),
                escapar(conteiner.getPosicao().getCamadaOperacional()),
                movimento.getRegistradoEm()
        );
    }

    private EquipamentoPatio localizarEquipamento(EquipamentoPatioRequisicaoDto requisicaoDto) {
        if (requisicaoDto.getId() != null) {
            return equipamentoPatioRepositorio.findById(requisicaoDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipamento não encontrado."));
        }
        Optional<EquipamentoPatio> existente = equipamentoPatioRepositorio.findByIdentificador(requisicaoDto.getIdentificador());
        return existente.orElseGet(EquipamentoPatio::new);
    }

    private ConteinerMapaDto converterConteiner(ConteinerPatio conteiner) {
        return new ConteinerMapaDto(
                conteiner.getId(),
                escapar(conteiner.getCodigo()),
                conteiner.getPosicao().getLinha(),
                conteiner.getPosicao().getColuna(),
                conteiner.getStatus(),
                escapar(conteiner.getCarga().getCodigo()),
                escapar(conteiner.getDestino()),
                escapar(conteiner.getPosicao().getCamadaOperacional())
        );
    }

    private EquipamentoMapaDto converterEquipamento(EquipamentoPatio equipamento) {
        return new EquipamentoMapaDto(
                equipamento.getId(),
                escapar(equipamento.getIdentificador()),
                equipamento.getTipoEquipamento(),
                equipamento.getLinha(),
                equipamento.getColuna(),
                equipamento.getStatusOperacional()
        );
    }

    private void publicarAtualizacaoTempoReal() {
        MapaPatioRespostaDto mapaAtual = consultarMapa(construirFiltro(List.of(), List.of(), List.of(), List.of(), List.of()));
        EventoMapaTempoRealDto evento = new EventoMapaTempoRealDto("ATUALIZACAO_MAPA", mapaAtual);
        messagingTemplate.convertAndSend(TOPICO_ATUALIZACOES, evento);
    }

    private String escapar(String valor) {
        if (valor == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }
}
