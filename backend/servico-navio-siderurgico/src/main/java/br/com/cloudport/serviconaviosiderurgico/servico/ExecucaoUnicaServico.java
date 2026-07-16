package br.com.cloudport.serviconaviosiderurgico.servico;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class ExecucaoUnicaServico {

    private static final String SQL_BLOQUEIO_POSTGRES =
            "SELECT pg_try_advisory_xact_lock(hashtext(?), hashtext(reverse(?)))";

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, ReentrantLock> bloqueiosLocais = new ConcurrentHashMap<>();

    public ExecucaoUnicaServico(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean executarSeDisponivel(String chave, Runnable operacao) {
        if (!StringUtils.hasText(chave)) {
            throw new IllegalArgumentException("A chave da execucao unica deve ser informada.");
        }
        if (operacao == null) {
            throw new IllegalArgumentException("A operacao da execucao unica deve ser informada.");
        }

        if (bancoPostgres()) {
            return executarComBloqueioPostgres(chave.trim(), operacao);
        }
        return executarComBloqueioLocal(chave.trim(), operacao);
    }

    private boolean executarComBloqueioPostgres(String chave, Runnable operacao) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("O bloqueio distribuido deve ser executado dentro de uma transacao.");
        }

        Boolean adquirido = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SQL_BLOQUEIO_POSTGRES)) {
                statement.setString(1, chave);
                statement.setString(2, chave);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() && resultSet.getBoolean(1);
                }
            }
        });

        if (!Boolean.TRUE.equals(adquirido)) {
            return false;
        }
        operacao.run();
        return true;
    }

    private boolean executarComBloqueioLocal(String chave, Runnable operacao) {
        ReentrantLock bloqueio = bloqueiosLocais.computeIfAbsent(chave, ignorada -> new ReentrantLock());
        if (!bloqueio.tryLock()) {
            return false;
        }
        try {
            operacao.run();
            return true;
        } finally {
            bloqueio.unlock();
        }
    }

    private boolean bancoPostgres() {
        Boolean resultado = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            String nomeBanco = connection.getMetaData().getDatabaseProductName();
            return nomeBanco != null && nomeBanco.toLowerCase(Locale.ROOT).contains("postgresql");
        });
        return Boolean.TRUE.equals(resultado);
    }
}
