package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.configuracoes.validacao.SanitizadorEntrada;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private static final String PAPEL_ROOT = "ROLE_ROOT";
    private static final List<String> PAPEIS_BASE = List.of(
            PAPEL_ROOT,
            "ROLE_ADMIN_PORTO",
            "ROLE_PLANEJADOR",
            "ROLE_OPERADOR_PATIO",
            "ROLE_OPERADOR_GATE",
            "ROLE_TRANSPORTADORA",
            "ROLE_INTEGRACAO_EXTERNA",
            "ROLE_SERVICE_NAVIO",
            "ROLE_SERVICE_SIDERURGICO"
    );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String emailRoot;
    private final String senhaRoot;

    public UserInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${cloudport.bootstrap.admin.email}") String emailRoot,
            @Value("${cloudport.bootstrap.admin.password}") String senhaRoot) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailRoot = SanitizadorEntrada.sanitizarLogin(emailRoot);
        SanitizadorEntrada.validarNovaSenha(senhaRoot);
        this.senhaRoot = senhaRoot;
    }

    @Override
    public void run(String... args) {
        PAPEIS_BASE.forEach(this::garantirPapel);
        garantirUsuarioRoot();
        sincronizarTodosOsPapeisDoRoot();
    }

    private void garantirPapel(String nomePapel) {
        jdbcTemplate.update(
                "INSERT INTO roles (name) " +
                        "SELECT ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = ?)",
                nomePapel,
                nomePapel);
    }

    private void garantirUsuarioRoot() {
        String senhaCodificada = passwordEncoder.encode(senhaRoot);

        jdbcTemplate.update(
                "INSERT INTO users (id, login, password, nome, transportadora_documento, transportadora_nome) " +
                        "SELECT ?, ?, ?, ?, NULL, NULL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = ?)",
                UUID.randomUUID(),
                emailRoot,
                senhaCodificada,
                "Root do sistema",
                emailRoot);

        jdbcTemplate.update(
                "UPDATE users " +
                        "SET password = ?, nome = ?, transportadora_documento = NULL, transportadora_nome = NULL " +
                        "WHERE login = ?",
                senhaCodificada,
                "Root do sistema",
                emailRoot);
    }

    private void sincronizarTodosOsPapeisDoRoot() {
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id, status) " +
                        "SELECT u.id, r.id, 'ATIVO' " +
                        "FROM users u " +
                        "CROSS JOIN roles r " +
                        "WHERE u.login = ? " +
                        "AND NOT EXISTS (" +
                        "    SELECT 1 " +
                        "    FROM user_roles ur " +
                        "    WHERE ur.user_id = u.id " +
                        "      AND ur.role_id = r.id" +
                        ")",
                emailRoot);

        jdbcTemplate.update(
                "UPDATE user_roles " +
                        "SET status = 'ATIVO' " +
                        "WHERE user_id = (SELECT id FROM users WHERE login = ?)",
                emailRoot);
    }
}
