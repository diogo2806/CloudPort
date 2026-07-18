package br.com.cloudport.servicogate.app.operacional;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicogate.exception.ApiError;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;

class GateOperacionalDatabaseExceptionHandlerTest {

    private final GateOperacionalDatabaseExceptionHandler handler =
            new GateOperacionalDatabaseExceptionHandler();

    @Test
    void deveRetornarConflitoQuandoCapacidadeDaJanelaEsgotou() {
        UncategorizedSQLException exception = excecaoBanco(
                "GATE_WINDOW_CAPACITY_EXHAUSTED",
                "P1001");

        ResponseEntity<ApiError> response = handler.tratarFalhaBanco(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("A janela do agendamento não possui capacidade disponível.");
    }

    @Test
    void deveRetornarEntidadeInvalidaQuandoAcessoEstaBloqueado() {
        UncategorizedSQLException exception = excecaoBanco(
                "GATE_ACCESS_BLOCKED",
                "P1002");

        ResponseEntity<ApiError> response = handler.tratarFalhaBanco(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("O acesso ao Gate está bloqueado por regra operacional.");
    }

    @Test
    void deveRetornarConflitoQuandoAgendamentoJaPossuiVisita() {
        UncategorizedSQLException exception = excecaoBanco(
                "duplicate key value violates unique constraint uk_truck_visit_agendamento",
                "23505");

        ResponseEntity<ApiError> response = handler.tratarFalhaBanco(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("O agendamento já possui uma truck visit aberta.");
    }

    @Test
    void deveRetornarEntidadeInvalidaQuandoReferenciaFoiRemovidaDuranteAbertura() {
        UncategorizedSQLException exception = excecaoBanco(
                "violates foreign key constraint truck_visit_motorista_id_fkey",
                "23503");

        ResponseEntity<ApiError> response = handler.tratarFalhaBanco(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Uma referência informada para a truck visit não está mais disponível.");
    }

    @Test
    void devePreservarErroInternoParaFalhaTecnicaNaoMapeada() {
        UncategorizedSQLException exception = excecaoBanco(
                "connection failure",
                "08006");

        ResponseEntity<ApiError> response = handler.tratarFalhaBanco(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Não foi possível concluir a operação de Gate.");
    }

    private UncategorizedSQLException excecaoBanco(String mensagem, String sqlState) {
        SQLException sqlException = new SQLException(mensagem, sqlState);
        return new UncategorizedSQLException("abrir truck visit", "INSERT INTO truck_visit", sqlException);
    }
}
