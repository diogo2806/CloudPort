package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.configuracoes.validacao.SanitizadorEntrada;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String emailAdministrador;
    private final String senhaAdministrador;

    public UserInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${cloudport.bootstrap.admin.email}") String emailAdministrador,
            @Value("${cloudport.bootstrap.admin.password}") String senhaAdministrador) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailAdministrador = SanitizadorEntrada.sanitizarLogin(emailAdministrador);
        SanitizadorEntrada.validarNovaSenha(senhaAdministrador);
        this.senhaAdministrador = senhaAdministrador;
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
        String senhaCodificada = passwordEncoder.encode(senhaAdministrador);

        jdbcTemplate.update(
                "INSERT INTO users (id, login, password, nome, transportadora_documento, transportadora_nome) " +
                        "SELECT ?, ?, ?, ?, NULL, NULL " +
                        "WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = ?)",
                UUID.randomUUID(),
                emailAdministrador,
                senhaCodificada,
                "Administrador do sistema",
                emailAdministrador);

        jdbcTemplate.update(
                "UPDATE users " +
                        "SET password = ? " +
                        "WHERE login = ? AND (password IS NULL OR password NOT LIKE '$2%')",
                senhaCodificada,
                emailAdministrador);

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
                emailAdministrador);
    }
}
