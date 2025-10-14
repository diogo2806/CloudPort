package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.dto.mapper.GateOperadorMapper;
import br.com.cloudport.servicogate.dto.operador.GateOperadorContatoDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorEventoDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorExcecaoDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorFilaDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorPainelDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorVeiculoDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.CanalEntrada;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import br.com.cloudport.servicogate.repository.GateEventRepository;
import br.com.cloudport.servicogate.repository.GateOcorrenciaRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class GateOperadorPainelService {

    private static final int HISTORICO_LIMITE_PADRAO = 50;

    private final AgendamentoRepository agendamentoRepository;
    private final GateEventRepository gateEventRepository;
    private final GateOcorrenciaRepository gateOcorrenciaRepository;

    public GateOperadorPainelService(AgendamentoRepository agendamentoRepository,
                                     GateEventRepository gateEventRepository,
                                     GateOcorrenciaRepository gateOcorrenciaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.gateEventRepository = gateEventRepository;
        this.gateOcorrenciaRepository = gateOcorrenciaRepository;
    }

    public GateOperadorPainelDTO montarPainel() {
        LocalDateTime agora = LocalDateTime.now();
        List<StatusAgendamento> statusConsiderados = List.of(
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.EM_ATENDIMENTO,
                StatusAgendamento.EM_EXECUCAO
        );
        List<Agendamento> agendamentos = agendamentoRepository
                .findByStatusInOrderByHorarioPrevistoChegadaAsc(statusConsiderados);

        List<GateOperadorVeiculoDTO> veiculosAtendimento = agendamentos.stream()
                .filter(agendamento -> agendamento.getStatus() == StatusAgendamento.EM_ATENDIMENTO)
                .map(agendamento -> toVeiculoDTO(agendamento,
                        calcularMinutos(agendamento.getHorarioRealChegada(), agora)))
                .collect(Collectors.toList());

        List<GateOperadorFilaDTO> filasEntrada = montarFilas(
                agendamentos.stream()
                        .filter(agendamento -> agendamento.getStatus() == StatusAgendamento.CONFIRMADO)
                        .collect(Collectors.toList()),
                true,
                agora
        );

        List<GateOperadorFilaDTO> filasSaida = montarFilas(
                agendamentos.stream()
                        .filter(agendamento -> agendamento.getStatus() == StatusAgendamento.EM_EXECUCAO)
                        .collect(Collectors.toList()),
                false,
                agora
        );

        List<GateOperadorEventoDTO> historico = listarEventosRecentes(HISTORICO_LIMITE_PADRAO);

        return new GateOperadorPainelDTO(
                filasEntrada,
                filasSaida,
                veiculosAtendimento,
                historico,
                agora
        );
    }

    public List<GateOperadorEventoDTO> listarEventosRecentes(int limite) {
        List<GateOperadorEventoDTO> eventosGate = gateEventRepository.findTop100ByOrderByRegistradoEmDesc()
                .stream()
                .map(GateOperadorMapper::toEventoDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<GateOperadorEventoDTO> ocorrencias = gateOcorrenciaRepository.findTop100ByOrderByRegistradoEmDesc()
                .stream()
                .map(GateOperadorMapper::toEventoDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return Stream.concat(eventosGate.stream(), ocorrencias.stream())
                .sorted(Comparator.comparing(GateOperadorEventoDTO::registradoEm,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limite)
                .collect(Collectors.toList());
    }

    private List<GateOperadorFilaDTO> montarFilas(List<Agendamento> agendamentos,
                                                  boolean entrada,
                                                  LocalDateTime agora) {
        if (agendamentos.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<Agendamento>> agrupados = agendamentos.stream()
                .collect(Collectors.groupingBy(agendamento -> identificarFila(agendamento, entrada)));

        return agrupados.entrySet().stream()
                .map(entry -> {
                    List<GateOperadorVeiculoDTO> veiculos = entry.getValue().stream()
                            .map(agendamento -> toVeiculoDTO(agendamento, calcularTempoFila(agendamento, entrada, agora)))
                            .collect(Collectors.toList());
                    long media = veiculos.stream()
                            .map(GateOperadorVeiculoDTO::tempoFilaMinutos)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0);
                    String id = gerarIdentificadorFila(entry.getKey(), entrada);
                    return new GateOperadorFilaDTO(
                            id,
                            entry.getKey(),
                            veiculos.size(),
                            media > 0 ? Math.round(media) : 0L,
                            veiculos
                    );
                })
                .sorted(Comparator.comparing(GateOperadorFilaDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private String identificarFila(Agendamento agendamento, boolean entrada) {
        JanelaAtendimento janela = agendamento.getJanelaAtendimento();
        if (janela != null && janela.getCanalEntrada() != null) {
            String canal = janela.getCanalEntrada().getDescricao();
            if (janela.getHoraInicio() != null) {
                return canal + " - " + janela.getHoraInicio();
            }
            return canal;
        }
        Transportadora transportadora = agendamento.getTransportadora();
        if (!entrada && transportadora != null && StringUtils.hasText(transportadora.getNome())) {
            return transportadora.getNome();
        }
        return entrada ? "Fila de entrada" : "Fila de saída";
    }

    private String gerarIdentificadorFila(String nome, boolean entrada) {
        String base = StringUtils.hasText(nome) ? nome : (entrada ? "entrada" : "saida");
        return base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private GateOperadorVeiculoDTO toVeiculoDTO(Agendamento agendamento, Long tempoFilaMinutos) {
        Veiculo veiculo = agendamento.getVeiculo();
        Transportadora transportadora = agendamento.getTransportadora();
        Motorista motorista = agendamento.getMotorista();
        JanelaAtendimento janela = agendamento.getJanelaAtendimento();
        CanalEntrada canal = janela != null ? janela.getCanalEntrada() : null;

        List<GateOperadorContatoDTO> contatos = montarContatos(motorista, transportadora);
        List<GateOperadorExcecaoDTO> excecoes = buscarExcecoes(agendamento);

        boolean podeImprimir = agendamento.getGatePass() != null &&
                StringUtils.hasText(agendamento.getGatePass().getToken());

        return new GateOperadorVeiculoDTO(
                veiculo != null ? veiculo.getId() : null,
                veiculo != null ? veiculo.getPlaca() : null,
                motorista != null ? motorista.getDocumento() : null,
                motorista != null ? motorista.getNome() : null,
                agendamento.getStatus() != null ? agendamento.getStatus().name() : null,
                agendamento.getStatus() != null ? agendamento.getStatus().getDescricao() : null,
                tempoFilaMinutos,
                canal != null ? canal.getDescricao() : null,
                transportadora != null ? transportadora.getNome() : null,
                contatos,
                excecoes,
                podeImprimir
        );
    }

    private Long calcularTempoFila(Agendamento agendamento, boolean entrada, LocalDateTime agora) {
        if (entrada) {
            return calcularMinutos(agendamento.getHorarioPrevistoChegada(), agora);
        }
        LocalDateTime referencia = Optional.ofNullable(agendamento.getHorarioRealChegada())
                .orElse(agendamento.getHorarioPrevistoChegada());
        return calcularMinutos(referencia, agora);
    }

    private Long calcularMinutos(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            return 0L;
        }
        long minutos = Duration.between(inicio, fim).toMinutes();
        return minutos < 0 ? 0L : minutos;
    }

    private List<GateOperadorContatoDTO> montarContatos(Motorista motorista, Transportadora transportadora) {
        List<GateOperadorContatoDTO> contatos = new ArrayList<>();
        if (motorista != null && StringUtils.hasText(motorista.getTelefone())) {
            contatos.add(new GateOperadorContatoDTO("TELEFONE", motorista.getTelefone(), "Motorista"));
        }
        if (transportadora != null && StringUtils.hasText(transportadora.getContato())) {
            String contato = transportadora.getContato();
            String tipo = contato.contains("@") ? "EMAIL" : "TELEFONE";
            contatos.add(new GateOperadorContatoDTO(tipo, contato, "Transportadora"));
        }
        return contatos;
    }

    private List<GateOperadorExcecaoDTO> buscarExcecoes(Agendamento agendamento) {
        if (agendamento.getGatePass() == null) {
            return Collections.emptyList();
        }
        return gateEventRepository.findByGatePassIdOrderByRegistradoEmAsc(agendamento.getGatePass().getId())
                .stream()
                .filter(evento -> evento.getMotivoExcecao() != null)
                .map(evento -> new GateOperadorExcecaoDTO(
                        evento.getMotivoExcecao().name(),
                        evento.getMotivoExcecao().getDescricao(),
                        GateOperadorMapper.nivelFromStatus(evento.getStatus()).name()
                ))
                .collect(Collectors.toList());
    }
}
