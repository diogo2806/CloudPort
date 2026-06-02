package br.com.cloudport.servicoautenticacao.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private static final String LOGIN_ADMIN = "gitpod";
    private static final String SENHA_ADMIN = "gitpod";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        garantirPapel("ROLE_ADMIN_PORTO");
        garantirPapel("ROLE_PLANEJADOR");
        garantirPapel("ROLE_OPERADOR_GATE");
        garantirPapel("ROLE_TRANSPORTADORA");
        garantirAdministradorInicial();
    }

    private void garantirPapel(String nomePapel) {
        jdbcTemplate.update(
                "INSERT INTO roles (name) " +
                        "SELECT ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = ?)",
                nomePapel,
                nomePapel);
    }

    private void garantirAdministradorInicial() {
        String senhaCodificada = passwordEncoder.encode(SENHA_ADMIN);

        jdbcTemplate.update(
                "INSERT INTO users (id, login, password, nome, transportadora_documento, transportadora_nome) " +
                        "SELECT RANDOM_UUID(), ?, ?, ?, NULL, NULL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = ?)",
                LOGIN_ADMIN,
                senhaCodificada,
                "Administrador do sistema",
                LOGIN_ADMIN);

        jdbcTemplate.update(
                "UPDATE users " +
                        "SET password = ? " +
                        "WHERE login = ? AND (password IS NULL OR password NOT LIKE '$2%')",
                senhaCodificada,
                LOGIN_ADMIN);

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id, status) " +
                        "SELECT u.id, r.id, 'ATIVO' " +
                        "FROM users u " +
                        "JOIN roles r ON r.name = ? " +
                        "WHERE u.login = ? " +
                        "AND NOT EXISTS (" +
                        "    SELECT 1 " +
                        "    FROM user_roles ur " +
                        "    WHERE ur.user_id = u.id " +
                        "      AND ur.role_id = r.id" +
                        ")",
                "ROLE_ADMIN_PORTO",
                LOGIN_ADMIN);
    }
}
