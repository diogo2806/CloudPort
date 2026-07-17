package br.com.cloudport.servicogate.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoBarcodeCicloCoordenador {

    private static final String CHAVE_EXECUCAO = "cloudport:gate:reconciliacao-barcode:noturna";
    private static final String SQL_REIVINDICAR_POSTGRES =
            "SELECT pg_try_advisory_lock(hashtext(?), hashtext(reverse(?)))";
    private static final String SQL_LIBERAR_POSTGRES =
            "SELECT pg_advisory_unlock(hashtext(?), hashtext(reverse(?)))";

    private final DataSource dataSource;
    private final ReentrantLock bloqueioLocal = new ReentrantLock();

    public ReconciliacaoBarcodeCicloCoordenador(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<ReivindicacaoCiclo> reivindicar() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (bancoPostgres(connection)) {
                if (!tentarBloqueioPostgres(connection)) {
                    connection.close();
                    return Optional.empty();
                }
                return Optional.of(new ReivindicacaoPostgres(connection));
            }

            connection.close();
            if (!bloqueioLocal.tryLock()) {
                return Optional.empty();
            }
            return Optional.of(new ReivindicacaoLocal(bloqueioLocal));
        } catch (SQLException ex) {
            fecharSilenciosamente(connection);
            throw new IllegalStateException(
                    "Não foi possível reivindicar o ciclo de reconciliação de barcode.", ex);
        }
    }

    private boolean tentarBloqueioPostgres(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_REIVINDICAR_POSTGRES)) {
            statement.setString(1, CHAVE_EXECUCAO);
            statement.setString(2, CHAVE_EXECUCAO);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private boolean bancoPostgres(Connection connection) throws SQLException {
        String nomeBanco = connection.getMetaData().getDatabaseProductName();
        return nomeBanco != null && nomeBanco.toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private void fecharSilenciosamente(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // A falha original da reivindicação deve ser preservada.
        }
    }

    public interface ReivindicacaoCiclo extends AutoCloseable {

        @Override
        void close();
    }

    private static final class ReivindicacaoPostgres implements ReivindicacaoCiclo {

        private final Connection connection;
        private final AtomicBoolean encerrada = new AtomicBoolean();

        private ReivindicacaoPostgres(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void close() {
            if (!encerrada.compareAndSet(false, true)) {
                return;
            }

            RuntimeException falha = null;
            try (PreparedStatement statement = connection.prepareStatement(SQL_LIBERAR_POSTGRES)) {
                statement.setString(1, CHAVE_EXECUCAO);
                statement.setString(2, CHAVE_EXECUCAO);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next() || !resultSet.getBoolean(1)) {
                        falha = new IllegalStateException(
                                "A instância não era mais proprietária do ciclo de reconciliação de barcode.");
                    }
                }
            } catch (SQLException ex) {
                falha = new IllegalStateException(
                        "Não foi possível liberar o ciclo de reconciliação de barcode.", ex);
            } finally {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    if (falha == null) {
                        falha = new IllegalStateException(
                                "Não foi possível devolver a conexão usada no ciclo de reconciliação de barcode.", ex);
                    } else {
                        falha.addSuppressed(ex);
                    }
                }
            }

            if (falha != null) {
                throw falha;
            }
        }
    }

    private static final class ReivindicacaoLocal implements ReivindicacaoCiclo {

        private final ReentrantLock bloqueio;
        private final AtomicBoolean encerrada = new AtomicBoolean();

        private ReivindicacaoLocal(ReentrantLock bloqueio) {
            this.bloqueio = bloqueio;
        }

        @Override
        public void close() {
            if (encerrada.compareAndSet(false, true)) {
                bloqueio.unlock();
            }
        }
    }
}
