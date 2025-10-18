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
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MapaPatioServico {

    private static final String TOPICO_ATUALIZACOES = "/topico/patio";

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final EquipamentoPatioRepositorio equipamentoPatioRepositorio;
    private final SimpMessagingTemplate messagingTemplate;

    public MapaPatioServico(ConteinerPatioRepositorio conteinerPatioRepositorio,
                             EquipamentoPatioRepositorio equipamentoPatioRepositorio,
                             SimpMessagingTemplate messagingTemplate) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.equipamentoPatioRepositorio = equipamentoPatioRepositorio;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public MapaPatioRespostaDto consultarMapa(MapaPatioFiltro filtro) {
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio.findAllByOrderByLinhaAscColunaAsc();
        List<EquipamentoPatio> equipamentos = equipamentoPatioRepositorio.findAllByOrderByTipoEquipamentoAscIdentificadorAsc();

        List<ConteinerMapaDto> conteineresFiltrados = conteineres.stream()
                .filter(conteiner -> filtrarConteiner(conteiner, filtro))
                .map(this::converterConteiner)
                .collect(Collectors.toList());

        List<EquipamentoMapaDto> equipamentosFiltrados = equipamentos.stream()
                .filter(equipamento -> filtrarEquipamento(equipamento, filtro))
                .map(this::converterEquipamento)
                .collect(Collectors.toList());

        int totalLinhas = conteineres.stream().map(ConteinerPatio::getLinha).max(Integer::compareTo)
                .orElse(0);
        int totalColunas = conteineres.stream().map(ConteinerPatio::getColuna).max(Integer::compareTo)
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
                .map(ConteinerPatio::getTipoCarga)
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> destinos = conteineres.stream()
                .map(ConteinerPatio::getDestino)
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> camadas = conteineres.stream()
                .map(ConteinerPatio::getCamadaOperacional)
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

    @Transactional
    public ConteinerMapaDto registrarOuAtualizarConteiner(ConteinerPatioRequisicaoDto requisicaoDto) {
        ConteinerPatio conteiner = localizarConteiner(requisicaoDto);
        conteiner.setCodigo(requisicaoDto.getCodigo());
        conteiner.setLinha(requisicaoDto.getLinha());
        conteiner.setColuna(requisicaoDto.getColuna());
        conteiner.setStatus(requisicaoDto.getStatus());
        conteiner.setTipoCarga(requisicaoDto.getTipoCarga());
        conteiner.setDestino(requisicaoDto.getDestino());
        conteiner.setCamadaOperacional(requisicaoDto.getCamadaOperacional());
        conteiner.setAtualizadoEm(requisicaoDto.gerarHorarioAtualizacao());

        ConteinerPatio salvo = conteinerPatioRepositorio.save(conteiner);
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

    private List<String> normalizarLista(List<String> valores) {
        return ValidacaoEntradaUtil.limparLista(valores).stream()
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private boolean filtrarConteiner(ConteinerPatio conteiner, MapaPatioFiltro filtro) {
        return verificarFiltro(filtro.getStatus(), conteiner.getStatus().name())
                && verificarFiltro(filtro.getTiposCarga(), conteiner.getTipoCarga())
                && verificarFiltro(filtro.getDestinos(), conteiner.getDestino())
                && verificarFiltro(filtro.getCamadasOperacionais(), conteiner.getCamadaOperacional());
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
                conteiner.getCodigo(),
                conteiner.getLinha(),
                conteiner.getColuna(),
                conteiner.getStatus(),
                conteiner.getTipoCarga(),
                conteiner.getDestino(),
                conteiner.getCamadaOperacional()
        );
    }

    private EquipamentoMapaDto converterEquipamento(EquipamentoPatio equipamento) {
        return new EquipamentoMapaDto(
                equipamento.getId(),
                equipamento.getIdentificador(),
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
}
