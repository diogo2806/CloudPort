DELETE FROM user_roles
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE login = 'gitpod'
      AND password = 'gitpod'
);

DELETE FROM users
WHERE login = 'gitpod'
  AND password = 'gitpod';
