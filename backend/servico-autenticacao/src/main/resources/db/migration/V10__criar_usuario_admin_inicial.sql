INSERT INTO roles (name)
SELECT 'ROLE_ADMIN_PORTO'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN_PORTO');

INSERT INTO roles (name)
SELECT 'ROLE_PLANEJADOR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_PLANEJADOR');

INSERT INTO roles (name)
SELECT 'ROLE_OPERADOR_GATE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_OPERADOR_GATE');

INSERT INTO roles (name)
SELECT 'ROLE_TRANSPORTADORA'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TRANSPORTADORA');

INSERT INTO users (id, login, password, nome, transportadora_documento, transportadora_nome)
SELECT CAST('11111111-1111-1111-1111-111111111111' AS UUID),
       'gitpod',
       'gitpod',
       'Administrador do sistema',
       NULL,
       NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE login = 'gitpod');

INSERT INTO user_roles (user_id, role_id, status)
SELECT CAST('11111111-1111-1111-1111-111111111111' AS UUID),
       r.id,
       'ATIVO'
FROM roles r
WHERE r.name = 'ROLE_ADMIN_PORTO'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = CAST('11111111-1111-1111-1111-111111111111' AS UUID)
        AND ur.role_id = r.id
  );
