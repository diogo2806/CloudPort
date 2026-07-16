# Deploy do monólito modular CloudPort

Este Compose executa o runtime `cloudport-monolito`, com Navio, Navio Siderúrgico, Yard, Gate, Rail, Autenticação e Visibilidade no mesmo processo. O diretório mantém o nome histórico para não quebrar pipelines e rollback.

## Runtime consolidado

A partir da raiz:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile monolito \
  up -d --build
```

As APIs e o Control Room ficam na mesma origem, por padrão `http://localhost:8086`.

O ambiente inclui:

- PostgreSQL 16;
- RabbitMQ;
- Redis;
- um JAR Spring Boot com os sete módulos;
- frontend React incorporado.

A conexão PostgreSQL preserva um schema e um histórico Flyway por módulo. Configure banco, JWT, RabbitMQ, Redis e origens CORS antes de usar fora do desenvolvimento local.

## Smoke automatizado

```bash
bash deploy/navio-monolito/smoke-test.sh
```

O smoke cria um ambiente descartável e valida:

1. construção e inicialização da imagem completa;
2. frontend e configuração dinâmica;
3. bloqueio `401` sem autenticação;
4. JWT e autorização;
5. persistência de Navio e Navio Siderúrgico;
6. criação de visita;
7. consulta local às work queues do Yard;
8. carregamento dos schemas e migrações incorporadas.

No smoke, jobs e consumidores são desativados para evitar processamento de fundo sem dados de integração. O script publica diagnóstico e remove containers, rede e volumes ao terminar.

## Coexistência com legado

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile monolito \
  --profile legado \
  up -d --build
```

O perfil legado disponível neste Compose mantém Navio e Navio Siderúrgico para comparação e rollback. Os demais deployments legados continuam nos manifests próprios de cada ambiente.

Durante coexistência:

- monólito: escrita, jobs e consumidores ativos;
- legados: escrita, jobs e consumidores desativados;
- cada rota aponta para um único backend;
- o volume PostgreSQL é preservado;
- credenciais e imagens legadas não são removidas.

## Corte e retorno

1. Validar os sete históricos Flyway.
2. Criar backup e registrar o ponto de restauração.
3. Iniciar o monólito e executar smoke funcional.
4. Direcionar as rotas para o monólito.
5. Manter legados sem escrita, jobs e consumidores durante observação.
6. Para retornar, retirar e parar o monólito antes de habilitar qualquer legado.
7. Reativar escrita, jobs e consumidores somente no runtime escolhido.
8. Não executar downgrade Flyway; o binário anterior deve ser compatível por `expand and contract`.

Detalhes: [`../../docs/operacao-corte-rollback-navio.md`](../../docs/operacao-corte-rollback-navio.md).
