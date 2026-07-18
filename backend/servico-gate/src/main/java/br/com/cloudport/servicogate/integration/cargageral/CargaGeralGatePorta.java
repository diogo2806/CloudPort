package br.com.cloudport.servicogate.integration.cargageral;

import java.math.BigDecimal;
import java.util.UUID;

public interface CargaGeralGatePorta {

    ReservaGateResposta reservar(ReservarGateRequest request);

    ReservaGateResposta confirmar(UUID reservaId, UUID commandId, String estagio, String usuario);

    ReservaGateResposta compensar(UUID reservaId, UUID commandId, String motivo, String usuario);

    record ReservarGateRequest(
            UUID commandId,
            String agendamentoCodigo,
            String blNumero,
            String deliveryOrder,
            UUID loteId,
            String tipoMovimento,
            String estagioConfirmacao,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario) {
    }

    record ReservaGateResposta(
            UUID id,
            String agendamentoCodigo,
            String status,
            String estagioConfirmacao) {
    }
}
