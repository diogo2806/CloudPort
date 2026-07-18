package br.com.cloudport.servicogate.app.operacional;

import br.com.cloudport.servicogate.exception.ApiError;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = GateOperacionalController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GateOperacionalDatabaseExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateOperacionalDatabaseExceptionHandler.class);

    private static final Map<String, RejeicaoOperacional> REJEICOES_POR_SQL_STATE = Map.of(
            "P1001", new RejeicaoOperacional(
                    HttpStatus.CONFLICT,
                    "A janela do agendamento não possui capacidade disponível."),
            "P1002", new RejeicaoOperacional(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O acesso ao Gate está bloqueado por regra operacional."),
            "P1003", new RejeicaoOperacional(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A referência informada não possui permissão ativa para este Gate."),
            "P1004", new RejeicaoOperacional(
                    HttpStatus.CONFLICT,
                    "A referência operacional está indisponível, expirada ou sem saldo."));

    private static final Set<String> CONSTRAINTS_REFERENCIA_TRUCK_VISIT = Set.of(
            "truck_visit_agendamento_id_fkey",
            "truck_visit_facility_id_fkey",
            "truck_visit_gate_id_fkey",
            "truck_visit_lane_id_fkey",
            "truck_visit_transportadora_id_fkey",
            "truck_visit_motorista_id_fkey",
            "truck_visit_veiculo_id_fkey",
            "truck_visit_stage_atual_id_fkey",
            "gate_transaction_booking_id_fkey",
            "gate_transaction_order_id_fkey",
            "gate_transaction_preadvice_id_fkey",
            "gate_transaction_stage_atual_id_fkey");

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> tratarFalhaBanco(DataAccessException exception) {
        RejeicaoOperacional rejeicao = identificarRejeicao(exception);
        if (rejeicao != null) {
            ApiError apiError = new ApiError(
                    rejeicao.status().value(),
                    rejeicao.mensagem(),
                    Collections.emptyList());
            return new ResponseEntity<>(apiError, rejeicao.status());
        }

        LOGGER.error("Falha técnica ao executar operação de Gate", exception);
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Não foi possível concluir a operação de Gate.",
                Collections.emptyList());
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private RejeicaoOperacional identificarRejeicao(Throwable throwable) {
        Throwable atual = throwable;
        while (atual != null) {
            if (atual instanceof SQLException sqlException) {
                RejeicaoOperacional porEstado = REJEICOES_POR_SQL_STATE.get(sqlException.getSQLState());
                if (porEstado != null) {
                    return porEstado;
                }

                RejeicaoOperacional porConstraint = identificarConstraint(
                        sqlException.getSQLState(),
                        sqlException.getMessage());
                if (porConstraint != null) {
                    return porConstraint;
                }
            }
            atual = atual.getCause();
        }
        return null;
    }

    private RejeicaoOperacional identificarConstraint(String sqlState, String mensagemTecnica) {
        if ("23505".equals(sqlState) && contem(mensagemTecnica, "uk_truck_visit_agendamento")) {
            return new RejeicaoOperacional(
                    HttpStatus.CONFLICT,
                    "O agendamento já possui uma truck visit aberta.");
        }

        if ("23503".equals(sqlState)
                && CONSTRAINTS_REFERENCIA_TRUCK_VISIT.stream()
                .anyMatch(constraint -> contem(mensagemTecnica, constraint))) {
            return new RejeicaoOperacional(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Uma referência informada para a truck visit não está mais disponível.");
        }
        return null;
    }

    private boolean contem(String valor, String trecho) {
        return valor != null && valor.contains(trecho);
    }

    private record RejeicaoOperacional(HttpStatus status, String mensagem) {
    }
}
