package br.com.cloudport.servicorail.ferrovia.locomotiva.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfiguracaoLocomotivaVisitaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.ConfirmacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.EntregaCustodiaLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.LiberacaoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.PlanejamentoEmbarqueLocomotivaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.dto.TransferenciaLocomotivaRespostaDto;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.StatusTransferenciaLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.TransferenciaLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.porta.ConsultaVisitaNavioPorta;
import br.com.cloudport.servicorail.ferrovia.locomotiva.repositorio.TransferenciaLocomotivaRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.TipoVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferenciaLocomotivaServico {

    private final TransferenciaLocomotivaRepositorio transferenciaRepositorio;
    private final VisitaTremRepositorio visitaTremRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;
    private final Optional<ConsultaVisitaNavioPorta> consultaVisitaNavioPorta;

    public TransferenciaLocomotivaServico(TransferenciaLocomotivaRepositorio transferenciaRepositorio,
                                           VisitaTremRepositorio visitaTremRepositorio,
                                           SanitizadorEntrada sanitizadorEntrada,
                                           Optional<ConsultaVisitaNavioPorta> consultaVisitaNavioPorta) {
        this.transferenciaRepositorio = transferenciaRepositorio;
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
        this.consultaVisitaNavioPorta = consultaVisitaNavioPorta;
    }

    @Transactional
    public TransferenciaLocomotivaRespostaDto configurarVisitaComoLocomotiva(
            Long visitaTremId,
            ConfiguracaoLocomotivaVisitaDto dto) {
        VisitaTrem visitaTrem = buscarVisita(visitaTremId);
        validarVisitaComoLocomotivaIsolada(visitaTrem);

        TransferenciaLocomotiva transferencia = transferenciaRepositorio.findById(visitaTremId)
                .orElseGet(TransferenciaLocomotiva::new);
        if (transferencia.getStatus() != null
                && transferencia.getStatus() != StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA) {
            throw erro(HttpStatus.CONFLICT,
                    "Os dados físicos da locomotiva não podem ser alterados após a entrega de custódia.");
        }

        visitaTrem.setTipoVisita(TipoVisitaTrem.LOCOMOTIVA_ISOLADA);
        visitaTremRepositorio.save(visitaTrem);

        transferencia.setVisitaTrem(visitaTrem);
        transferencia.setFabricante(textoOpcional(dto.getFabricante(), "fabricante", 80));
        transferencia.setModelo(textoOpcional(dto.getModelo(), "modelo", 80));
        transferencia.setNumeroSerie(textoOpcional(dto.getNumeroSerie(), "número de série", 80));
        transferencia.setPesoToneladas(decimalPositivo(dto.getPesoToneladas(), "peso em toneladas"));
        transferencia.setComprimentoMetros(decimalPositivo(dto.getComprimentoMetros(), "comprimento"));
        transferencia.setLarguraMetros(decimalPositivo(dto.getLarguraMetros(), "largura"));
        transferencia.setAlturaMetros(decimalPositivo(dto.getAlturaMetros(), "altura"));
        transferencia.setObservacoes(textoOpcional(dto.getObservacoes(), "observações", 1000));
        transferencia.setStatus(StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA);
        return resposta(transferenciaRepositorio.save(transferencia));
    }

    @Transactional(readOnly = true)
    public List<TransferenciaLocomotivaRespostaDto> listar() {
        return transferenciaRepositorio.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(TransferenciaLocomotivaRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferenciaLocomotivaRespostaDto consultar(Long visitaTremId) {
        return resposta(buscar(visitaTremId));
    }

    @Transactional
    public TransferenciaLocomotivaRespostaDto registrarEntregaCustodia(Long visitaTremId,
                                                                         EntregaCustodiaLocomotivaDto dto) {
        TransferenciaLocomotiva transferencia = buscar(visitaTremId);
        exigirStatus(transferencia, StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA);
        transferencia.setNomeMaquinista(textoObrigatorio(dto.getNomeMaquinista(), "nome do maquinista", 120));
        transferencia.setDocumentoEntrega(textoObrigatorio(dto.getDocumentoEntrega(), "documento de entrega", 80));
        transferencia.setResponsavelTerminal(textoObrigatorio(dto.getResponsavelTerminal(),
                "responsável do terminal", 120));
        transferencia.setEntregueEm(dataOuAgora(dto.getEntregueEm()));
        atualizarObservacoes(transferencia, dto.getObservacoes());
        transferencia.setStatus(StatusTransferenciaLocomotiva.SOB_CUSTODIA_TERMINAL);
        return resposta(transferenciaRepositorio.save(transferencia));
    }

    @Transactional
    public TransferenciaLocomotivaRespostaDto planejarEmbarque(Long visitaTremId,
                                                                PlanejamentoEmbarqueLocomotivaDto dto) {
        TransferenciaLocomotiva transferencia = buscar(visitaTremId);
        exigirStatus(transferencia, StatusTransferenciaLocomotiva.SOB_CUSTODIA_TERMINAL);
        Long visitaNavioId = Optional.ofNullable(dto.getVisitaNavioId())
                .filter(valor -> valor > 0)
                .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST, "A visita de navio deve ser informada."));
        String codigoVisitaNavio = textoObrigatorio(dto.getCodigoVisitaNavio(),
                "código da visita de navio", 60).toUpperCase(Locale.ROOT);
        if (consultaVisitaNavioPorta.isPresent()
                && !consultaVisitaNavioPorta.get().existe(visitaNavioId, codigoVisitaNavio)) {
            throw erro(HttpStatus.NOT_FOUND,
                    "A visita de navio informada não existe ou não corresponde ao código fornecido.");
        }
        transferencia.setVisitaNavioId(visitaNavioId);
        transferencia.setCodigoVisitaNavio(codigoVisitaNavio);
        transferencia.setModalidadeEmbarque(Optional.ofNullable(dto.getModalidadeEmbarque())
                .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST, "A modalidade de embarque deve ser informada.")));
        transferencia.setDeckPlanejado(textoObrigatorio(dto.getDeckPlanejado(), "deck planejado", 80));
        transferencia.setPosicaoPlanejada(textoObrigatorio(dto.getPosicaoPlanejada(),
                "posição planejada", 120));
        atualizarObservacoes(transferencia, dto.getObservacoes());
        transferencia.setStatus(StatusTransferenciaLocomotiva.PLANEJADA_PARA_EMBARQUE);
        return resposta(transferenciaRepositorio.save(transferencia));
    }

    @Transactional
    public TransferenciaLocomotivaRespostaDto liberarEmbarque(Long visitaTremId,
                                                               LiberacaoEmbarqueLocomotivaDto dto) {
        TransferenciaLocomotiva transferencia = buscar(visitaTremId);
        exigirStatus(transferencia, StatusTransferenciaLocomotiva.PLANEJADA_PARA_EMBARQUE);
        if (!dto.isFreioEstacionamentoAplicado()
                || !dto.isBateriasIsoladas()
                || !dto.isCombustivelProtegido()
                || !dto.isCalcosInstalados()
                || !dto.isPlanoAmarracaoAprovado()) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A locomotiva só pode ser liberada após a conclusão integral do checklist de segurança.");
        }
        transferencia.setFreioEstacionamentoAplicado(true);
        transferencia.setBateriasIsoladas(true);
        transferencia.setCombustivelProtegido(true);
        transferencia.setCalcosInstalados(true);
        transferencia.setPlanoAmarracaoAprovado(true);
        transferencia.setLiberadaEm(dataOuAgora(dto.getLiberadaEm()));
        transferencia.setStatus(StatusTransferenciaLocomotiva.PRONTA_PARA_EMBARQUE);
        return resposta(transferenciaRepositorio.save(transferencia));
    }

    @Transactional
    public TransferenciaLocomotivaRespostaDto confirmarEmbarque(Long visitaTremId,
                                                                 ConfirmacaoEmbarqueLocomotivaDto dto) {
        TransferenciaLocomotiva transferencia = buscar(visitaTremId);
        exigirStatus(transferencia, StatusTransferenciaLocomotiva.PRONTA_PARA_EMBARQUE);
        LocalDateTime embarcadaEm = dataOuAgora(dto.getEmbarcadaEm());
        if (transferencia.getLiberadaEm() != null && embarcadaEm.isBefore(transferencia.getLiberadaEm())) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "O embarque não pode ocorrer antes da liberação operacional.");
        }
        transferencia.setEmbarcadaEm(embarcadaEm);
        transferencia.setPosicaoReal(textoObrigatorio(dto.getPosicaoReal(), "posição real a bordo", 120));
        atualizarObservacoes(transferencia, dto.getObservacoes());
        transferencia.setStatus(StatusTransferenciaLocomotiva.EMBARCADA);

        VisitaTrem visitaTrem = transferencia.getVisitaTrem();
        visitaTrem.setStatusVisita(StatusVisitaTrem.CONCLUIDO);
        visitaTremRepositorio.save(visitaTrem);
        return resposta(transferenciaRepositorio.save(transferencia));
    }

    private VisitaTrem buscarVisita(Long visitaTremId) {
        if (visitaTremId == null || visitaTremId <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O identificador da visita ferroviária é inválido.");
        }
        return visitaTremRepositorio.buscarPorIdComListas(visitaTremId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Visita de trem não encontrada."));
    }

    private void validarVisitaComoLocomotivaIsolada(VisitaTrem visitaTrem) {
        if (visitaTrem.getStatusVisita() == StatusVisitaTrem.PARTIU) {
            throw erro(HttpStatus.CONFLICT,
                    "Uma visita ferroviária já encerrada por partida não pode ser embarcada no navio.");
        }
        if (!visitaTrem.getListaVagoes().isEmpty()
                || !visitaTrem.getListaCarga().isEmpty()
                || !visitaTrem.getListaDescarga().isEmpty()) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A visita da locomotiva representa o próprio trem e não pode possuir vagões ou contêineres.");
        }
    }

    private TransferenciaLocomotiva buscar(Long visitaTremId) {
        if (visitaTremId == null || visitaTremId <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O identificador da visita ferroviária é inválido.");
        }
        return transferenciaRepositorio.findById(visitaTremId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND,
                        "A visita ferroviária não está configurada como locomotiva isolada."));
    }

    private void exigirStatus(TransferenciaLocomotiva transferencia,
                               StatusTransferenciaLocomotiva esperado) {
        if (transferencia.getStatus() != esperado) {
            throw erro(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT,
                            "A operação exige o status %s, mas a visita da locomotiva está em %s.",
                            esperado, transferencia.getStatus()));
        }
    }

    private BigDecimal decimalPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s deve ser maior que zero.", campo));
        }
        return valor;
    }

    private String textoObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String normalizado = normalizarTexto(valor, campo, tamanhoMaximo);
        if (!StringUtils.hasText(normalizado)) {
            throw erro(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s é obrigatório.", campo));
        }
        return normalizado;
    }

    private String textoOpcional(String valor, String campo, int tamanhoMaximo) {
        String normalizado = normalizarTexto(valor, campo, tamanhoMaximo);
        return StringUtils.hasText(normalizado) ? normalizado : null;
    }

    private String normalizarTexto(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw erro(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s contém caracteres inválidos.", campo));
        }
        if (!StringUtils.hasText(limpo)) {
            return null;
        }
        String normalizado = limpo.trim();
        if (normalizado.length() > tamanhoMaximo) {
            throw erro(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s deve ter no máximo %d caracteres.",
                            campo, tamanhoMaximo));
        }
        return normalizado;
    }

    private void atualizarObservacoes(TransferenciaLocomotiva transferencia, String novasObservacoes) {
        String observacoes = textoOpcional(novasObservacoes, "observações", 1000);
        if (observacoes != null) {
            transferencia.setObservacoes(observacoes);
        }
    }

    private LocalDateTime dataOuAgora(LocalDateTime data) {
        return Optional.ofNullable(data)
                .orElseGet(LocalDateTime::now)
                .truncatedTo(ChronoUnit.SECONDS);
    }

    private TransferenciaLocomotivaRespostaDto resposta(TransferenciaLocomotiva transferencia) {
        return TransferenciaLocomotivaRespostaDto.deEntidade(transferencia);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
