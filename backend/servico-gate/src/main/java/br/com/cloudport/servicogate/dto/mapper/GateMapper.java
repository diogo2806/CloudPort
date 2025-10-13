package br.com.cloudport.servicogate.dto.mapper;

import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.dto.GateEventDTO;
import br.com.cloudport.servicogate.dto.GatePassDTO;
import br.com.cloudport.servicogate.dto.JanelaAtendimentoDTO;
import br.com.cloudport.servicogate.dto.MotoristaDTO;
import br.com.cloudport.servicogate.dto.TransportadoraDTO;
import br.com.cloudport.servicogate.dto.VeiculoDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.CanalEntrada;
import br.com.cloudport.servicogate.model.enums.MotivoExcecao;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GateMapper {

    private GateMapper() {
    }

    public static TransportadoraDTO toTransportadoraDTO(Transportadora entity) {
        if (entity == null) {
            return null;
        }
        return new TransportadoraDTO(entity.getId(), entity.getNome(), entity.getDocumento(), entity.getContato());
    }

    public static MotoristaDTO toMotoristaDTO(Motorista entity) {
        if (entity == null) {
            return null;
        }
        return new MotoristaDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDocumento(),
                entity.getTelefone(),
                entity.getTransportadora() != null ? entity.getTransportadora().getId() : null,
                entity.getTransportadora() != null ? entity.getTransportadora().getNome() : null
        );
    }

    public static VeiculoDTO toVeiculoDTO(Veiculo entity) {
        if (entity == null) {
            return null;
        }
        return new VeiculoDTO(
                entity.getId(),
                entity.getPlaca(),
                entity.getModelo(),
                entity.getTipo(),
                entity.getTransportadora() != null ? entity.getTransportadora().getId() : null,
                entity.getTransportadora() != null ? entity.getTransportadora().getNome() : null
        );
    }

    public static JanelaAtendimentoDTO toJanelaAtendimentoDTO(JanelaAtendimento entity) {
        if (entity == null) {
            return null;
        }
        CanalEntrada canal = entity.getCanalEntrada();
        return new JanelaAtendimentoDTO(
                entity.getId(),
                entity.getData(),
                entity.getHoraInicio(),
                entity.getHoraFim(),
                entity.getCapacidade(),
                canal != null ? canal.name() : null,
                canal != null ? canal.getDescricao() : null
        );
    }

    public static DocumentoAgendamentoDTO toDocumentoAgendamentoDTO(DocumentoAgendamento entity) {
        if (entity == null) {
            return null;
        }
        return new DocumentoAgendamentoDTO(
                entity.getId(),
                entity.getTipoDocumento(),
                entity.getNumero(),
                entity.getUrlDocumento(),
                entity.getNomeArquivo(),
                entity.getContentType(),
                entity.getTamanhoBytes(),
                entity.getUltimaRevalidacao()
        );
    }

    public static GateEventDTO toGateEventDTO(GateEvent entity) {
        if (entity == null) {
            return null;
        }
        StatusGate statusGate = entity.getStatus();
        MotivoExcecao motivo = entity.getMotivoExcecao();
        return new GateEventDTO(
                entity.getId(),
                statusGate != null ? statusGate.name() : null,
                statusGate != null ? statusGate.getDescricao() : null,
                motivo != null ? motivo.name() : null,
                motivo != null ? motivo.getDescricao() : null,
                entity.getObservacao(),
                entity.getUsuarioResponsavel(),
                entity.getRegistradoEm()
        );
    }

    public static GatePassDTO toGatePassDTO(GatePass entity) {
        if (entity == null) {
            return null;
        }
        StatusGate statusGate = entity.getStatus();
        List<GateEventDTO> eventos = safeList(entity.getEventos()).stream()
                .map(GateMapper::toGateEventDTO)
                .collect(Collectors.toList());
        return new GatePassDTO(
                entity.getId(),
                entity.getCodigo(),
                statusGate != null ? statusGate.name() : null,
                statusGate != null ? statusGate.getDescricao() : null,
                entity.getDataEntrada(),
                entity.getDataSaida(),
                eventos,
                entity.getToken()
        );
    }

    public static AgendamentoDTO toAgendamentoDTO(Agendamento entity) {
        if (entity == null) {
            return null;
        }
        TipoOperacao tipoOperacao = entity.getTipoOperacao();
        StatusAgendamento statusAgendamento = entity.getStatus();
        JanelaAtendimento janela = entity.getJanelaAtendimento();
        return new AgendamentoDTO(
                entity.getId(),
                entity.getCodigo(),
                tipoOperacao != null ? tipoOperacao.name() : null,
                tipoOperacao != null ? tipoOperacao.getDescricao() : null,
                statusAgendamento != null ? statusAgendamento.name() : null,
                statusAgendamento != null ? statusAgendamento.getDescricao() : null,
                entity.getTransportadora() != null ? entity.getTransportadora().getId() : null,
                entity.getTransportadora() != null ? entity.getTransportadora().getNome() : null,
                entity.getMotorista() != null ? entity.getMotorista().getId() : null,
                entity.getMotorista() != null ? entity.getMotorista().getNome() : null,
                entity.getVeiculo() != null ? entity.getVeiculo().getId() : null,
                entity.getVeiculo() != null ? entity.getVeiculo().getPlaca() : null,
                janela != null ? janela.getId() : null,
                janela != null ? janela.getData() : null,
                janela != null ? janela.getHoraInicio() : null,
                janela != null ? janela.getHoraFim() : null,
                entity.getHorarioPrevistoChegada(),
                entity.getHorarioPrevistoSaida(),
                entity.getHorarioRealChegada(),
                entity.getHorarioRealSaida(),
                entity.getObservacoes(),
                safeList(entity.getDocumentos()).stream()
                        .map(GateMapper::toDocumentoAgendamentoDTO)
                        .collect(Collectors.toList()),
                toGatePassDTO(entity.getGatePass())
        );
    }

    private static <T> List<T> safeList(List<T> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

    public static List<TransportadoraDTO> toTransportadoraDTO(List<Transportadora> entities) {
        return safeStream(entities).map(GateMapper::toTransportadoraDTO).collect(Collectors.toList());
    }

    public static List<MotoristaDTO> toMotoristaDTO(List<Motorista> entities) {
        return safeStream(entities).map(GateMapper::toMotoristaDTO).collect(Collectors.toList());
    }

    public static List<VeiculoDTO> toVeiculoDTO(List<Veiculo> entities) {
        return safeStream(entities).map(GateMapper::toVeiculoDTO).collect(Collectors.toList());
    }

    public static List<DocumentoAgendamentoDTO> toDocumentoAgendamentoDTO(List<DocumentoAgendamento> entities) {
        return safeStream(entities).map(GateMapper::toDocumentoAgendamentoDTO).collect(Collectors.toList());
    }

    private static <T> java.util.stream.Stream<T> safeStream(List<T> collection) {
        return Objects.requireNonNullElse(collection, Collections.<T>emptyList()).stream();
    }
}
