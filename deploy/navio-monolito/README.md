# Deploy do runtime unificado de Navio

Este Compose permite executar Navio e Navio Siderurgico no runtime `cloudport-monolito-navio` ou, para comparacao e rollback em desenvolvimento/homologacao, nos dois deployments legados.

## Runtime unificado

Execute a partir da raiz do repositorio:

```bash
docker compose -f deploy/navio-monolito/docker-compose.yml --profile monolito up --build
```

O Control Room e as APIs ficam na mesma origem, por padrao em `http://localhost:8086`. O portal principal deve configurar `navioControlRoomUrl` com essa origem.

O frontend incorporado recebe sua configuracao em tempo de execucao por `GET /assets/configuracao.json`. Configure as origens autorizadas do portal por `CONTROL_ROOM_TRUSTED_PARENT_ORIGINS` e o CORS do backend por `SECURITY_CORS_ALLOWED_ORIGINS`.

A conexao unica usa os schemas `cloudport_navio` e `cloudport_siderurgico`, mantendo historicos Flyway independentes. Defina as variaveis de banco, JWT, credencial interna e URL do Yard no ambiente antes de usar fora do desenvolvimento local.

## Smoke automatizado

Execute a partir da raiz do repositorio:

```bash
bash deploy/navio-monolito/smoke-test.sh
```

O teste cria um ambiente descartavel com PostgreSQL, runtime unificado e um mock autenticado do Yard. Ele valida:

1. construcao e inicializacao da imagem com o frontend React incorporado;
2. configuracao dinamica do Control Room na mesma origem;
3. bloqueio `401` para acesso sem autenticacao;
4. autenticacao JWT e persistencia nos dois modulos;
5. cadastro canonico local, extensao siderurgica e criacao de visita;
6. chamada real do runtime ao contrato de work queues do Yard com `X-CloudPort-Service-Key`.

O script gera credenciais temporarias, exibe logs quando ocorre falha e remove containers, rede de projeto e volume ao terminar. O mock existe somente no overlay `docker-compose.smoke.yml` e nao deve ser usado como dependencia operacional.

## Comparacao com os deployments legados

```bash
docker compose -f deploy/navio-monolito/docker-compose.yml --profile legado up --build
```

O perfil legado publica Navio em `8084` e Navio Siderurgico em `8085`. Nao execute os perfis `monolito` e `legado` simultaneamente contra o mesmo banco, pois os jobs agendados e comandos operacionais seriam duplicados.

## Corte e retorno

1. Pare os deployments ativos antes de trocar o perfil.
2. Preserve o volume PostgreSQL e os nomes dos schemas.
3. Inicie o novo perfil e valide login, endpoints, jobs, Control Room e integracao com o Yard.
4. Em caso de retorno, pare o runtime unificado antes de iniciar o perfil legado.
5. Nao aponte ambientes existentes sem validar previamente a compatibilidade das migracoes aplicadas nas duas direcoes.
