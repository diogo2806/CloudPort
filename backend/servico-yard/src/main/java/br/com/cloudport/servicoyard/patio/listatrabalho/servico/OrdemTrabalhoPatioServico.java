package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadeOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.FilaOrdemTrabalhoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import br.com.cloudport.servicoyard.patio.servico.ValidadorYardPlacementService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdemTrabalhoPatioServico {

    private static final Duration DURACAO_RESERVA_REMANEJAMENTO = Duration.ofHours(12);

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoPatioRepositorio;
    private final MapaPatioServico mapaPatioServico;
    private final OtimizadorRotasPatioServico otimizadorRotas;
    private final ValidadorYardPlacementService validadorYardPlacement;

    public OrdemTrabalhoPatioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                      ConteinerPatioRepositorio conteinerRepositorio,
                                      PosicaoPatioRepositorio posicaoPatioRepositorio,
                                      MapaPatioServico mapaPatioServico,
                                      OtimizadorRotasPatioServico otimizadorRotas,
                                      ValidadorYardPlacementService validadorYardPlacement) {
        this.ordemRepositorio = ordemRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoPatioRepositorio = posicaoPatioRepositorio;
        this.mapaPatioServico = mapaPatioServico;
        this.otimizadorRotas = otimizadorRotas;
        this.validadorYardPlacement = validadorYardPlacement;
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdens(StatusOrdemTrabalhoPatio status) {
        List<StatusOrdemTrabalhoPatio> filtros = status != null
                ? List.of(status)
                : new ArrayList<>(EnumSet.allOf(StatusOrdemTrabalhoPatio.class));
        return ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(filtros).stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensPorVisitaNavio(Long visitaNavioId) {
        return ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId).stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FilaOrdemTrabalhoPatioDto> listarFilasPorVisitaNavio(Long visitaNavioId) {
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId);
        Map<String, List<OrdemTrabalhoPatio>> agrupadas = ordens.stream()
                .collect(Collectors.groupingBy(this::chaveFilaOperacional, LinkedHashMap::new, Collectors.toList()));

        return agrupadas.entrySet().stream()
                .map(entry -> montarFila(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FilaOrdemTrabalhoPatioDto::getSequenciaInicial,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensSemCobertura(Long visitaNavioId) {
        return ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId).stream()
                .filter(this::semCoberturaOperacional)
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensOtimizadas(StatusOrdemTrabalhoPatio status) {
        List<OrdemTrabalhoPatio> ordens = status != null
                ? ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(status)
                : ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(
                new ArrayList<>(EnumSet.allOf(StatusOrdemTrabalhoPatio.class)));

        if (ordens.isEmpty() || status != StatusOrdemTrabalhoPatio.PENDENTE) {
            return ordens.stream()
                    .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                    .toList();
        }

        List<OrdemTrabalhoPatio> ordensOtimizadas = otimizadorRotas.otimizarRota();
        return ordensOtimizadas.stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensOtimizadasComDualCycling() {
        List<OrdemTrabalhoPatio> ordensOtimizadas = otimizadorRotas.otimizarRotaComProximidade();
        return ordensOtimizadas.stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatio> listarOrdensOriginais(StatusOrdemTrabalhoPatio status) {
        return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(status);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto registrarOrdem(OrdemTrabalhoPatioRequisicaoDto dto) {
        if (dto.getTipoMovimento() == TipoMovimentoPatio.REMANEJAMENTO) {
            return registrarOuReutilizarRemanejamento(dto);
        }
        return criarOrdem(dto, false, null, false);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto registrarOuReutilizarOrdemNavio(OrdemTrabalhoPatioRequisicaoDto dto) {
        if (dto.getVisitaNavioId() == null || dto.getItemOperacaoNavioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A ordem de navio deve informar visitaNavioId e itemOperacaoNavioId.");
        }
        Optional<OrdemTrabalhoPatio> existente = ordemRepositorio
                .findFirstByVisitaNavioIdAndItemOperacaoNavioIdAndStatusOrdemInOrderByCriadoEmAsc(
                        dto.getVisitaNavioId(), dto.getItemOperacaoNavioId(), statusAtivos());
        if (existente.isPresent()) {
            return OrdemTrabalhoPatioRespostaDto.deEntidade(existente.get());
        }
        return criarOrdem(dto, true, null, false);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto registrarOuReutilizarRemanejamento(
            OrdemTrabalhoPatioRequisicaoDto dto) {
        validarCamposObrigatorios(dto);
        String chaveIdempotencia = dto.getChaveIdempotencia().trim();
        Optional<OrdemTrabalhoPatio> existente = ordemRepositorio.findByChaveIdempotencia(chaveIdempotencia);
        if (existente.isPresent()) {
            return OrdemTrabalhoPatioRespostaDto.deEntidade(existente.get());
        }
        return criarOrdem(dto, false, chaveIdempotencia, true);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto atualizarPrioridade(Long id, AtualizacaoPrioridadeOrdemTrabalhoDto dto) {
        OrdemTrabalhoPatio ordem = ordemRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de trabalho não encontrada."));
        int prioridade = dto.prioridadeBuscaEfetiva() ? 0 : dto.getPrioridadeOperacional();
        ordem.setPrioridadeOperacional(prioridade);
        ordem.setAtualizadoEm(LocalDateTime.now());
        return OrdemTrabalhoPatioRespostaDto.deEntidade(ordemRepositorio.save(ordem));
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto suspender(Long id) {
        return alterarStatus(id, StatusOrdemTrabalhoPatio.SUSPENSA);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto retomar(Long id) {
        return alterarStatus(id, StatusOrdemTrabalhoPatio.PENDENTE);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto atualizarStatus(Long id, AtualizacaoStatusOrdemTrabalhoDto dto) {
        StatusOrdemTrabalhoPatio novoStatus = Optional.ofNullable(dto.getStatusOrdem())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O novo status deve ser informado."));
        return alterarStatus(id, novoStatus);
    }

    private OrdemTrabalhoPatioRespostaDto alterarStatus(Long id, StatusOrdemTrabalhoPatio novoStatus) {
        OrdemTrabalhoPatio ordem = ordemRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de trabalho não encontrada."));
        validarTransicaoStatus(ordem.getStatusOrdem(), novoStatus);
        if (ordem.getStatusOrdem() == novoStatus) {
            return OrdemTrabalhoPatioRespostaDto.deEntidade(ordem);
        }
        ordem.setStatusOrdem(novoStatus);
        ordem.setAtualizadoEm(LocalDateTime.now());
        if (novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            ordem.setConcluidoEm(LocalDateTime.now());
            aplicarAtualizacaoInventario(ordem);
        }
        OrdemTrabalhoPatio atualizado = ordemRepositorio.save(ordem);
        if (novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA
                || novoStatus == StatusOrdemTrabalhoPatio.CANCELADA) {
            liberarReservaDestino(atualizado);
        }
        return OrdemTrabalhoPatioRespostaDto.deEntidade(atualizado);
    }

    private OrdemTrabalhoPatioRespostaDto criarOrdem(OrdemTrabalhoPatioRequisicaoDto dto,
                                                       boolean origemNavio,
                                                       String chaveIdempotencia,
                                                       boolean reservarDestino) {
        validarCamposObrigatorios(dto);
        PosicaoPatio posicaoDestino = validarDestinoPatio(
                dto,
                origemNavio || dto.getTipoMovimento() == TipoMovimentoPatio.REMANEJAMENTO,
                chaveIdempotencia);
        if (reservarDestino) {
            reservarDestino(posicaoDestino, dto, chaveIdempotencia);
        }
        String codigoNormalizado = dto.getCodigoConteiner().toUpperCase(Locale.ROOT);
        if (dto.getItemOperacaoNavioId() != null
                && ordemRepositorio.existsByItemOperacaoNavioIdAndStatusOrdemIn(dto.getItemOperacaoNavioId(), statusAtivos())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma ordem ativa para este item de operação de navio.");
        }
        if (ordemRepositorio.existsByCodigoConteinerIgnoreCaseAndStatusOrdemIn(codigoNormalizado, statusAtivos())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma ordem pendente ou em execução para este contêiner.");
        }
        ConteinerPatio conteinerExistente = conteinerRepositorio.findByCodigoIgnoreCase(codigoNormalizado).orElse(null);
        LocalDateTime agora = LocalDateTime.now();
        String tipoCargaNormalizada = Optional.ofNullable(dto.getTipoCarga())
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .orElse(null);

        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                conteinerExistente,
                codigoNormalizado,
                tipoCargaNormalizada,
                dto.getDestino(),
                dto.getLinhaDestino(),
                dto.getColunaDestino(),
                dto.getCamadaDestino(),
                dto.getTipoMovimento(),
                StatusOrdemTrabalhoPatio.PENDENTE,
                dto.getStatusConteinerDestino(),
                agora,
                agora
        );
        ordem.setVisitaNavioId(dto.getVisitaNavioId());
        ordem.setItemOperacaoNavioId(dto.getItemOperacaoNavioId());
        ordem.setPlanoEstivaNavioId(dto.getPlanoEstivaNavioId());
        ordem.setTipoOrigem(StringUtils.hasText(dto.getTipoOrigem()) ? dto.getTipoOrigem().toUpperCase(Locale.ROOT) : null);
        ordem.setTipoDestino(StringUtils.hasText(dto.getTipoDestino()) ? dto.getTipoDestino().toUpperCase(Locale.ROOT) : null);
        ordem.setSequenciaNavio(dto.getSequenciaNavio());
        ordem.setPrioridadeOperacional(dto.getPrioridadeOperacional());
        ordem.setChaveIdempotencia(chaveIdempotencia);
        OrdemTrabalhoPatio salvo = ordemRepositorio.save(ordem);
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salvo);
    }

    private void aplicarAtualizacaoInventario(OrdemTrabalhoPatio ordem) {
        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(ordem.getCodigoConteiner());
        requisicao.setLinha(ordem.getLinhaDestino());
        requisicao.setColuna(ordem.getColunaDestino());
        requisicao.setStatus(ordem.getStatusConteinerDestino());
        requisicao.setTipoCarga(ordem.getTipoCarga());
        requisicao.setDestino(ordem.getDestino());
        requisicao.setCamadaOperacional(ordem.getCamadaDestino());
        mapaPatioServico.registrarOuAtualizarConteiner(requisicao);
        conteinerRepositorio.findByCodigoIgnoreCase(ordem.getCodigoConteiner())
                .ifPresent(ordem::setConteiner);
    }

    private void validarCamposObrigatorios(OrdemTrabalhoPatioRequisicaoDto dto) {
        if (!StringUtils.hasText(dto.getCodigoConteiner())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O código do contêiner é obrigatório.");
        }
        if (dto.getLinhaDestino() == null || dto.getLinhaDestino() < 0
                || dto.getColunaDestino() == null || dto.getColunaDestino() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As coordenadas da posição de destino devem ser válidas.");
        }
        if (!StringUtils.hasText(dto.getDestino())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O destino da carga é obrigatório.");
        }
        if (!StringUtils.hasText(dto.getCamadaDestino())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A camada operacional é obrigatória.");
        }
        if (dto.getTipoMovimento() == TipoMovimentoPatio.REMOCAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo de movimento de remoção deve ser tratado pelo módulo de expedição.");
        }
        if (dto.getStatusConteinerDestino() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O status final do contêiner é obrigatório e deve ser válido.");
        }
        if (dto.getTipoMovimento() == TipoMovimentoPatio.REMANEJAMENTO
                && !StringUtils.hasText(dto.getChaveIdempotencia())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A chave de idempotência é obrigatória para remanejamento.");
        }
    }

    private PosicaoPatio validarDestinoPatio(OrdemTrabalhoPatioRequisicaoDto dto,
                                              boolean exigirPosicaoReal,
                                              String chaveIdempotencia) {
        Optional<PosicaoPatio> posicaoOptional = posicaoPatioRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                dto.getLinhaDestino(), dto.getColunaDestino(), dto.getCamadaDestino());
        if (posicaoOptional.isEmpty()) {
            if (exigirPosicaoReal) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Posição de pátio inexistente para a ordem.");
            }
            return null;
        }

        PosicaoPatio posicao = posicaoOptional.get();
        LocalDateTime agora = LocalDateTime.now();
        if (posicao.getReservaChave() != null && !posicao.possuiReservaAtiva(agora)) {
            posicao.liberarReserva();
            posicaoPatioRepositorio.save(posicao);
        }
        if (posicao.possuiReservaAtiva(agora)
                && !posicao.getReservaChave().equals(chaveIdempotencia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Posição de pátio reservada por outra ordem.");
        }

        validarRestricoesPosicao(posicao, dto);
        validarOcupacao(posicao, dto);
        validarApoioPilha(posicao);

        boolean destinoComOrdemAtiva = ordemRepositorio
                .existsByLinhaDestinoAndColunaDestinoAndCamadaDestinoIgnoreCaseAndStatusOrdemIn(
                        dto.getLinhaDestino(), dto.getColunaDestino(), dto.getCamadaDestino(), statusAtivos());
        if (destinoComOrdemAtiva) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Posição de pátio já destinada a outra ordem ativa.");
        }

        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(dto.getCodigoConteiner());
        requisicao.setLinha(dto.getLinhaDestino());
        requisicao.setColuna(dto.getColunaDestino());
        requisicao.setCamadaOperacional(dto.getCamadaDestino());
        requisicao.setStatus(dto.getStatusConteinerDestino());
        requisicao.setTipoCarga(dto.getTipoCarga());
        requisicao.setDestino(dto.getDestino());
        try {
            validadorYardPlacement.validarAlocacao(requisicao);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return posicao;
    }

    private void validarRestricoesPosicao(PosicaoPatio posicao, OrdemTrabalhoPatioRequisicaoDto dto) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Posição de pátio indisponível para alocação.");
        }
        Integer nivel = extrairNivelCamada(posicao.getCamadaOperacional());
        if (nivel != null && posicao.getCamadaMaxima() != null && nivel > posicao.getCamadaMaxima()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A camada excede o limite configurado da posição.");
        }
        if (nivel != null && posicao.getCapacidadePilha() != null && nivel > posicao.getCapacidadePilha()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A camada excede a capacidade configurada da pilha.");
        }
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(dto.getCodigoConteiner()).orElse(null);
        if (conteiner != null
                && conteiner.getPesoToneladas() != null
                && posicao.getPesoMaximoToneladas() != null
                && conteiner.getPesoToneladas().compareTo(posicao.getPesoMaximoToneladas()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O peso do contêiner excede o limite da posição.");
        }
        if (StringUtils.hasText(dto.getTipoCarga()) && StringUtils.hasText(posicao.getTiposCargaPermitidos())) {
            String tipo = dto.getTipoCarga().trim().toUpperCase(Locale.ROOT);
            boolean permitido = List.of(posicao.getTiposCargaPermitidos().split("[,;|]"))
                    .stream()
                    .map(String::trim)
                    .map(valor -> valor.toUpperCase(Locale.ROOT))
                    .anyMatch(tipo::equals);
            if (!permitido) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O tipo de carga não é permitido na posição de destino.");
            }
        }
    }

    private void validarOcupacao(PosicaoPatio posicao, OrdemTrabalhoPatioRequisicaoDto dto) {
        boolean ocupadaPorOutraCarga = conteinerRepositorio.findAll().stream()
                .filter(conteiner -> conteiner.getPosicao() != null)
                .filter(conteiner -> conteiner.getPosicao().getId().equals(posicao.getId()))
                .anyMatch(conteiner -> !conteiner.getCodigo().equalsIgnoreCase(dto.getCodigoConteiner()));
        if (ocupadaPorOutraCarga) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Posição de pátio já ocupada por outra carga.");
        }
    }

    private void validarApoioPilha(PosicaoPatio posicao) {
        Integer nivel = extrairNivelCamada(posicao.getCamadaOperacional());
        if (nivel == null || nivel <= 1) {
            return;
        }
        List<ConteinerPatio> pilha = conteinerRepositorio.findByPosicaoLinhaAndPosicaoColuna(
                posicao.getLinha(), posicao.getColuna());
        for (int nivelInferior = 1; nivelInferior < nivel; nivelInferior++) {
            int nivelEsperado = nivelInferior;
            boolean ocupado = pilha.stream()
                    .filter(conteiner -> conteiner.getPosicao() != null)
                    .map(conteiner -> extrairNivelCamada(conteiner.getPosicao().getCamadaOperacional()))
                    .anyMatch(nivelAtual -> nivelAtual != null && nivelAtual == nivelEsperado);
            if (!ocupado) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A posição de destino criaria uma lacuna na pilha real.");
            }
        }
    }

    private void reservarDestino(PosicaoPatio posicao,
                                  OrdemTrabalhoPatioRequisicaoDto dto,
                                  String chaveIdempotencia) {
        if (posicao == null || !StringUtils.hasText(chaveIdempotencia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível reservar uma posição real para o remanejamento.");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (posicao.possuiReservaAtiva(agora)
                && !chaveIdempotencia.equals(posicao.getReservaChave())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Posição de pátio reservada por outra ordem.");
        }
        posicao.reservar(
                chaveIdempotencia,
                dto.getCodigoConteiner().toUpperCase(Locale.ROOT),
                agora.plus(DURACAO_RESERVA_REMANEJAMENTO));
        posicaoPatioRepositorio.save(posicao);
    }

    private void liberarReservaDestino(OrdemTrabalhoPatio ordem) {
        if (!StringUtils.hasText(ordem.getChaveIdempotencia())) {
            return;
        }
        posicaoPatioRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                        ordem.getLinhaDestino(), ordem.getColunaDestino(), ordem.getCamadaDestino())
                .filter(posicao -> ordem.getChaveIdempotencia().equals(posicao.getReservaChave()))
                .ifPresent(posicao -> {
                    posicao.liberarReserva();
                    posicaoPatioRepositorio.save(posicao);
                });
    }

    private Integer extrairNivelCamada(String camada) {
        if (!StringUtils.hasText(camada)) {
            return null;
        }
        String digitos = camada.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digitos)) {
            return null;
        }
        try {
            return Integer.valueOf(digitos);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void validarTransicaoStatus(StatusOrdemTrabalhoPatio statusAtual,
                                         StatusOrdemTrabalhoPatio novoStatus) {
        if (statusAtual == novoStatus) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.CANCELADA
                && statusAtual != StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.BLOQUEADA
                && statusAtual != StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.SUSPENSA
                && statusAtual == StatusOrdemTrabalhoPatio.EM_EXECUCAO) {
            return;
        }
        if (statusAtual == StatusOrdemTrabalhoPatio.PENDENTE
                && (novoStatus == StatusOrdemTrabalhoPatio.EM_EXECUCAO
                || novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA
                || novoStatus == StatusOrdemTrabalhoPatio.SUSPENSA)) {
            return;
        }
        if (statusAtual == StatusOrdemTrabalhoPatio.EM_EXECUCAO
                && novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if ((statusAtual == StatusOrdemTrabalhoPatio.SUSPENSA || statusAtual == StatusOrdemTrabalhoPatio.BLOQUEADA)
                && novoStatus == StatusOrdemTrabalhoPatio.PENDENTE) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format(Locale.ROOT,
                        "A transição de status de %s para %s não é permitida.",
                        statusAtual, novoStatus));
    }

    private List<StatusOrdemTrabalhoPatio> statusAtivos() {
        return List.of(
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.SUSPENSA,
                StatusOrdemTrabalhoPatio.BLOQUEADA
        );
    }

    private String chaveFilaOperacional(OrdemTrabalhoPatio ordem) {
        return valor(ordem.getDestino(), "SEM_BERCO")
                + "|" + valor(ordem.getCamadaDestino(), "SEM_ZONA")
                + "|" + ordem.getStatusOrdem().name();
    }

    private FilaOrdemTrabalhoPatioDto montarFila(String chave, List<OrdemTrabalhoPatio> ordens) {
        OrdemTrabalhoPatio primeira = ordens.get(0);
        String[] partes = chave.split("\\|");
        List<OrdemTrabalhoPatioRespostaDto> respostas = ordens.stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
        return new FilaOrdemTrabalhoPatioDto(
                chave,
                "VISITA_BERCO_ZONA_STATUS",
                primeira.getVisitaNavioId(),
                partes.length > 0 ? partes[0] : null,
                partes.length > 1 ? partes[1] : null,
                ordens.stream().map(OrdemTrabalhoPatio::getSequenciaNavio).filter(java.util.Objects::nonNull).min(Integer::compareTo).orElse(null),
                primeira.getStatusOrdem(),
                ordens.size(),
                respostas
        );
    }

    private boolean semCoberturaOperacional(OrdemTrabalhoPatio ordem) {
        return !StringUtils.hasText(ordem.getTipoDestino())
                || ordem.getPrioridadeOperacional() == null
                || ordem.getSequenciaNavio() == null;
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : padrao;
    }
}
