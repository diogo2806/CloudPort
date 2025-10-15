package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.GateOperadorEventoDTO;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GateOcorrencia;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.NivelEvento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import org.springframework.util.StringUtils;

public final class GateOperadorMapper {

    private GateOperadorMapper() {
    }

    public static GateOperadorEventoDTO toEventoDTO(GateEvent event) {
        if (event == null || event.getGatePass() == null || event.getGatePass().getAgendamento() == null) {
            return null;
        }
        Agendamento agendamento = event.getGatePass().getAgendamento();
        Veiculo veiculo = agendamento.getVeiculo();
        Transportadora transportadora = agendamento.getTransportadora();
        StatusGate statusGate = event.getStatus();
        String tipo = statusGate != null ? statusGate.getDescricao() : "Evento de gate";
        String descricao = StringUtils.hasText(event.getObservacao()) ? event.getObservacao() : tipo;
        NivelEvento nivel = nivelFromStatus(statusGate);
        return new GateOperadorEventoDTO(
                event.getId(),
                tipo,
                descricao,
                nivel.name(),
                event.getRegistradoEm(),
                veiculo != null ? veiculo.getId() : null,
                veiculo != null ? veiculo.getPlaca() : null,
                transportadora != null ? transportadora.getNome() : null,
                event.getUsuarioResponsavel()
        );
    }

    public static GateOperadorEventoDTO toEventoDTO(GateOcorrencia ocorrencia) {
        if (ocorrencia == null) {
            return null;
        }
        Veiculo veiculo = ocorrencia.getVeiculo();
        Transportadora transportadora = ocorrencia.getTransportadora();
        String tipo = ocorrencia.getTipo() != null ? ocorrencia.getTipo().getDescricao() : "Ocorrência";
        NivelEvento nivel = ocorrencia.getNivel() != null ? ocorrencia.getNivel() : NivelEvento.INFO;
        return new GateOperadorEventoDTO(
                ocorrencia.getId(),
                tipo,
                ocorrencia.getDescricao(),
                nivel.name(),
                ocorrencia.getRegistradoEm(),
                veiculo != null ? veiculo.getId() : null,
                veiculo != null ? veiculo.getPlaca() : null,
                transportadora != null ? transportadora.getNome() : null,
                ocorrencia.getUsuarioResponsavel()
        );
    }

    public static NivelEvento nivelFromStatus(StatusGate status) {
        if (status == null) {
            return NivelEvento.INFO;
        }
        return switch (status) {
            case RETIDO -> NivelEvento.CRITICA;
            case LIBERADO, FINALIZADO -> NivelEvento.INFO;
            case EM_PROCESSAMENTO, AGUARDANDO_ENTRADA -> NivelEvento.OPERACIONAL;
        };
    }
}
