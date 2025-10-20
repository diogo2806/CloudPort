package br.com.cloudport.servicogate.app.cidadao.centralacao;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoService;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.AcaoAgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.CentralAcaoAgendamentoRespostaDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.DocumentoPendenteDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.SituacaoPatioDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.UsuarioCentralAcaoDTO;
import br.com.cloudport.servicogate.app.cidadao.centralacao.dto.VisaoCompletaAgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import br.com.cloudport.servicogate.security.UserInfoResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class CentralAcaoAgendamentoService {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int LIMITE_CARTOES = 20;

    private final AgendamentoService agendamentoService;
    private final AutenticacaoClient autenticacaoClient;
    private final ClienteStatusPatio clienteStatusPatio;

    public CentralAcaoAgendamentoService(AgendamentoService agendamentoService,
                                         AutenticacaoClient autenticacaoClient,
                                         ClienteStatusPatio clienteStatusPatio) {
        this.agendamentoService = agendamentoService;
        this.autenticacaoClient = autenticacaoClient;
        this.clienteStatusPatio = clienteStatusPatio;
    }

    public CentralAcaoAgendamentoRespostaDTO montarVisaoCompleta(String authorizationHeader) {
        Page<AgendamentoDTO> pagina = agendamentoService.buscar(null, null,
                PageRequest.of(0, LIMITE_CARTOES, Sort.by(Sort.Direction.ASC, "horarioPrevistoChegada")));

        Optional<StatusPatioResposta> situacaoPatio = clienteStatusPatio.consultarStatus(authorizationHeader);
        Optional<UserInfoResponse> usuarioAtual = buscarUsuarioAtual(authorizationHeader);

        List<VisaoCompletaAgendamentoDTO> visoes = pagina.getContent().stream()
                .sorted(Comparator.comparing((AgendamentoDTO dto) ->
                        Optional.ofNullable(dto.getHorarioPrevistoChegada()).orElse(LocalDateTime.MAX)))
                .map(dto -> montarVisao(dto, situacaoPatio.orElse(null)))
                .collect(Collectors.toList());

        CentralAcaoAgendamentoRespostaDTO resposta = new CentralAcaoAgendamentoRespostaDTO();
        resposta.setAgendamentos(visoes);
        usuarioAtual.map(this::converterUsuario).ifPresent(resposta::setUsuario);
        situacaoPatio.map(this::converterSituacaoPatio).ifPresent(resposta::setSituacaoPatio);
        return resposta;
    }

    public VisaoCompletaAgendamentoDTO montarVisaoPorId(Long id, String authorizationHeader) {
        AgendamentoDTO agendamento = agendamentoService.buscarPorId(id);
        Optional<StatusPatioResposta> situacaoPatio = clienteStatusPatio.consultarStatus(authorizationHeader);
        return montarVisao(agendamento, situacaoPatio.orElse(null));
    }

    private VisaoCompletaAgendamentoDTO montarVisao(AgendamentoDTO agendamento, StatusPatioResposta situacaoPatio) {
        if (agendamento == null) {
            return new VisaoCompletaAgendamentoDTO();
        }

        String codigoSanitizado = HtmlUtils.htmlEscape(agendamento.getCodigo());
        String statusSanitizado = HtmlUtils.htmlEscape(agendamento.getStatus());
        String statusDescricao = HtmlUtils.htmlEscape(agendamento.getStatusDescricao());
        String tipoOperacaoDescricao = HtmlUtils.htmlEscape(agendamento.getTipoOperacaoDescricao());
        String placaVeiculo = HtmlUtils.htmlEscape(agendamento.getPlacaVeiculo());
        String transportadoraNome = HtmlUtils.htmlEscape(agendamento.getTransportadoraNome());
        String motoristaNome = HtmlUtils.htmlEscape(agendamento.getMotoristaNome());

        AcaoAgendamentoDTO acao = definirAcaoPrincipal(agendamento);
        String mensagemOrientacao = construirMensagemOrientacao(agendamento, acao, situacaoPatio);

        VisaoCompletaAgendamentoDTO visao = new VisaoCompletaAgendamentoDTO();
        visao.setAgendamentoId(agendamento.getId());
        visao.setCodigo(codigoSanitizado);
        visao.setStatus(statusSanitizado);
        visao.setStatusDescricao(statusDescricao);
        visao.setTipoOperacaoDescricao(tipoOperacaoDescricao);
        visao.setHorarioPrevistoChegada(formatarDataHora(agendamento.getHorarioPrevistoChegada()));
        visao.setHorarioPrevistoSaida(formatarDataHora(agendamento.getHorarioPrevistoSaida()));
        visao.setPlacaVeiculo(placaVeiculo);
        visao.setTransportadoraNome(transportadoraNome);
        visao.setMotoristaNome(motoristaNome);
        visao.setJanelaData(formatarData(agendamento.getDataJanela()));
        visao.setJanelaHoraInicio(formatarHora(agendamento.getHoraInicioJanela()));
        visao.setJanelaHoraFim(formatarHora(agendamento.getHoraFimJanela()));
        visao.setMensagemOrientacao(HtmlUtils.htmlEscape(mensagemOrientacao));
        visao.setAcaoPrincipal(acao);
        visao.setDocumentosPendentes(mapearDocumentosPendentes(agendamento.getDocumentos()));
        return visao;
    }

    private List<DocumentoPendenteDTO> mapearDocumentosPendentes(List<DocumentoAgendamentoDTO> documentos) {
        if (documentos == null) {
            return Collections.emptyList();
        }
        return documentos.stream()
                .filter(Objects::nonNull)
                .filter(doc -> !"VALIDADO".equalsIgnoreCase(doc.getStatusValidacao()))
                .map(doc -> new DocumentoPendenteDTO(
                        doc.getId(),
                        HtmlUtils.htmlEscape(doc.getNomeArquivo()),
                        HtmlUtils.htmlEscape(doc.getTipoDocumento()),
                        HtmlUtils.htmlEscape(Objects.requireNonNullElse(doc.getMensagemValidacao(),
                                doc.getStatusValidacaoDescricao()))))
                .collect(Collectors.toList());
    }

    private String construirMensagemOrientacao(AgendamentoDTO agendamento,
                                                AcaoAgendamentoDTO acao,
                                                StatusPatioResposta situacaoPatio) {
        StringBuilder mensagem = new StringBuilder();
        String tituloAcao = null;
        if (acao != null && StringUtils.hasText(acao.getTitulo())) {
            tituloAcao = HtmlUtils.htmlUnescape(acao.getTitulo());
        }
        if (StringUtils.hasText(tituloAcao)) {
            mensagem.append("Próxima ação: ").append(tituloAcao);
        }
        if (agendamento.getHorarioPrevistoChegada() != null) {
            if (mensagem.length() > 0) {
                mensagem.append(" • ");
            }
            mensagem.append("Janela prevista às ")
                    .append(HORA_FORMATTER.format(agendamento.getHorarioPrevistoChegada().toLocalTime()));
        }
        if (situacaoPatio != null && StringUtils.hasText(situacaoPatio.getDescricao())) {
            if (mensagem.length() > 0) {
                mensagem.append(" • ");
            }
            mensagem.append("Pátio: ").append(situacaoPatio.getDescricao());
        }
        return mensagem.toString();
    }

    private AcaoAgendamentoDTO definirAcaoPrincipal(AgendamentoDTO agendamento) {
        StatusAgendamento status = interpretarStatus(agendamento.getStatus());
        if (status == null) {
            return null;
        }
        String rotaConfirmacao = String.format("/gate/agendamentos/%d/confirmar-chegada", agendamento.getId());
        switch (status) {
            case PENDENTE:
            case CONFIRMADO:
                return new AcaoAgendamentoDTO(
                        "CONFIRMAR_CHEGADA",
                        HtmlUtils.htmlEscape("Confirmar chegada"),
                        HtmlUtils.htmlEscape("Avise ao gate que você já está no local e aguarde liberação."),
                        "POST",
                        rotaConfirmacao,
                        true
                );
            case EM_ATENDIMENTO:
                return new AcaoAgendamentoDTO(
                        "AGUARDAR_LIBERACAO",
                        HtmlUtils.htmlEscape("Aguardar liberação"),
                        HtmlUtils.htmlEscape("Aguarde a atualização do status do gate pass pelo operador."),
                        "GET",
                        null,
                        false
                );
            case EM_EXECUCAO:
                return new AcaoAgendamentoDTO(
                        "ACOMPANHAR_OPERACAO",
                        HtmlUtils.htmlEscape("Acompanhar operação"),
                        HtmlUtils.htmlEscape("Siga as orientações no local até a conclusão do atendimento."),
                        "GET",
                        null,
                        false
                );
            default:
                return new AcaoAgendamentoDTO(
                        "FINALIZADO",
                        HtmlUtils.htmlEscape("Operação finalizada"),
                        HtmlUtils.htmlEscape("Revise os documentos e prepare-se para o próximo agendamento."),
                        "GET",
                        null,
                        false
                );
        }
    }

    private StatusAgendamento interpretarStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return StatusAgendamento.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Optional<UserInfoResponse> buscarUsuarioAtual(String authorizationHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
            Jwt token = jwtAuthenticationToken.getToken();
            String login = token.getSubject();
            if (StringUtils.hasText(login)) {
                return autenticacaoClient.buscarUsuario(login, authorizationHeader);
            }
        }
        return Optional.empty();
    }

    private UsuarioCentralAcaoDTO converterUsuario(UserInfoResponse response) {
        if (response == null) {
            return null;
        }
        return new UsuarioCentralAcaoDTO(
                HtmlUtils.htmlEscape(response.getLogin()),
                HtmlUtils.htmlEscape(response.getNome()),
                HtmlUtils.htmlEscape(response.getPerfil()),
                HtmlUtils.htmlEscape(response.getTransportadoraDocumento()),
                HtmlUtils.htmlEscape(response.getTransportadoraNome())
        );
    }

    private SituacaoPatioDTO converterSituacaoPatio(StatusPatioResposta resposta) {
        if (resposta == null) {
            return null;
        }
        return new SituacaoPatioDTO(
                HtmlUtils.htmlEscape(resposta.getStatus()),
                HtmlUtils.htmlEscape(resposta.getDescricao()),
                HtmlUtils.htmlEscape(resposta.getVerificadoEm())
        );
    }

    private String formatarData(LocalDate data) {
        return data != null ? DATA_FORMATTER.format(data) : null;
    }

    private String formatarHora(LocalTime hora) {
        return hora != null ? HORA_FORMATTER.format(hora) : null;
    }

    private String formatarDataHora(LocalDateTime dataHora) {
        return dataHora != null ? DATA_HORA_FORMATTER.format(dataHora) : null;
    }
}
