# Operação de corte e rollback do runtime de Navio

Este runbook cobre o primeiro corte do monólito modular, formado pelos módulos `servico-navio` e `servico-navio-siderurgico` no runtime `cloudport-monolito-navio`.

## Invariantes do corte

1. Somente um runtime pode aceitar comandos de escrita para Navio e Navio Siderúrgico.
2. Somente uma instância executa cada job agendado, mesmo durante sobreposição temporária de deployments.
3. Somente um deployment do Yard pode manter os consumidores RabbitMQ ativos durante a coexistência.
4. O runtime consolidado usa `CadastroNavioPorta` local e não realiza HTTP entre os dois módulos incorporados.
5. Os schemas `cloudport_navio` e `cloudport_siderurgico` continuam pertencendo aos respectivos módulos.
6. Os históricos `flyway_schema_history` permanecem independentes em cada schema.
7. Rollback troca o binário e o roteamento; não executa downgrade de banco.

## Controles implementados

- `CLOUDPORT_WRITES_ENABLED=false` coloca os deployments legados em modo somente leitura e responde `503` para `POST`, `PUT`, `PATCH` e `DELETE`.
- `CLOUDPORT_JOBS_ENABLED=false` remove os jobs siderúrgicos do contexto legado.
- `CLOUDPORT_CONSUMERS_ENABLED=false` impede que o deployment correspondente do Yard inicie os listeners COPRAR, COARRI e de movimentação ferroviária.
- Os jobs de reconciliação e sincronização usam `pg_try_advisory_xact_lock`, evitando execução simultânea em instâncias que compartilham o mesmo PostgreSQL.
- O Compose mantém o runtime monolítico como escritor e executor e inicia os serviços legados com escrita e jobs desativados por padrão.
- Testes ArchUnit impedem ciclos, acesso direto ao domínio/repository de outro módulo e uso do cliente HTTP legado pelo runtime monolítico.
- Testes de configuração verificam que todos os consumidores RabbitMQ do Yard respeitam o controle central de inicialização.
- Logs estruturados, métricas e tracing permitem correlacionar visita, item, reserva, ordem, work queue e equipamento durante o corte.

## Validações antes do corte

1. Fazer backup consistente do banco e registrar o ponto de restauração.
2. Confirmar que a branch de implantação contém todas as migrações já aplicadas no ambiente.
3. Executar o build completo e o smoke do Compose.
4. Confirmar que os dois Flyway executam `validate` sem migrações pendentes.
5. Comparar as quantidades essenciais dos dois schemas antes e depois de iniciar o runtime consolidado.
6. Confirmar que o proxy possui uma única regra de destino para cada rota.
7. Confirmar que o Yard está acessível e aceita `X-CloudPort-Service-Key`, `X-Correlation-Id` e `traceparent`.
8. Confirmar que apenas o deployment responsável está com `CLOUDPORT_CONSUMERS_ENABLED=true`.
9. Consultar `/actuator/prometheus` e confirmar a publicação de `cloudport_operacao_total` e `cloudport_operacao_duracao_seconds`.

Consultas mínimas de conferência:

```sql
SELECT COUNT(*) FROM cloudport_navio.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_siderurgico.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_navio.navio;
SELECT COUNT(*) FROM cloudport_siderurgico.navio_siderurgico;
SELECT COUNT(*) FROM cloudport_siderurgico.visita_navio;
```

Os nomes das tabelas operacionais devem ser ajustados somente quando uma migração do módulo proprietário os alterar.

## Corte com coexistência controlada

O Compose permite iniciar os dois perfis para observação, mantendo o legado sem escrita e sem jobs:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile monolito \
  --profile legado \
  up -d --build
```

Durante essa janela:

- o proxy aponta rotas funcionais somente para `cloudport-monolito-navio`;
- os serviços legados podem atender apenas consultas;
- tentativas de escrita no legado retornam `503`;
- jobs legados não são registrados;
- somente um deployment do Yard inicia os consumidores RabbitMQ;
- o bloqueio PostgreSQL protege contra uma configuração incorreta que habilite dois schedulers;
- o mesmo `correlationId` e `traceId` deve aparecer nos logs do runtime monolítico e do Yard para a mesma operação.

Depois do smoke funcional, parar os containers legados sem remover o volume PostgreSQL:

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  stop servico-navio servico-navio-siderurgico
```

## Critérios de aprovação

- frontend e configuração dinâmica disponíveis;
- rotas de Navio, Navio Siderúrgico e Visita carregadas no mesmo contexto;
- chamadas sem autenticação rejeitadas;
- JWT e perfis operacionais preservados;
- cadastro canônico acessado pela porta local;
- criação e consulta persistidas nos schemas existentes;
- fluxo completo visita, item, reserva, ordem, work queue, job list, sincronização e relatório integrado validado;
- integração com Yard autenticada e com correlação/tracing propagados;
- nenhum job duplicado nos logs;
- nenhum consumidor RabbitMQ duplicado no Yard;
- nenhuma escrita recebida pelos deployments legados;
- métricas operacionais disponíveis no Prometheus;
- Flyway sem erro de checksum, versão ou migração pendente.

## Rollback da aplicação

O rollback não desfaz migrações. Ele reativa o binário legado sobre os mesmos schemas, desde que a versão anterior seja compatível com as migrações já aplicadas.

1. Retirar o runtime monolítico do proxy.
2. Parar o runtime monolítico antes de reativar qualquer escritor legado.
3. Iniciar o perfil legado com escrita e jobs explicitamente habilitados:

```bash
LEGACY_NAVIO_WRITES_ENABLED=true \
LEGACY_NAVIO_JOBS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  up -d --build servico-navio servico-navio-siderurgico
```

4. Executar health checks e consultas de leitura.
5. Direcionar o proxy para os endpoints legados.
6. Executar uma escrita controlada e verificar auditoria, dados e integração com Yard.
7. Confirmar que somente o deployment escolhido do Yard está com `CLOUDPORT_CONSUMERS_ENABLED=true`.
8. Manter o monólito parado até a causa do rollback ser corrigida.

Nunca habilitar escrita ou jobs legados enquanto o monólito ainda estiver recebendo comandos. Nunca habilitar consumidores RabbitMQ em dois deployments do Yard ao mesmo tempo.

## Compatibilidade Flyway

As migrações do período de coexistência devem seguir `expand and contract`:

1. adicionar estruturas antes de remover ou renomear estruturas existentes;
2. manter colunas antigas enquanto o binário legado puder ser reativado;
3. preencher dados novos de forma idempotente;
4. evitar `DROP`, redução de tamanho, alteração incompatível de tipo e `NOT NULL` sem valor padrão no mesmo corte;
5. aplicar mudanças destrutivas somente após o fim formal da janela de rollback;
6. nunca alterar o conteúdo de uma migração já aplicada;
7. corrigir uma migração publicada somente por uma nova versão;
8. validar os dois históricos antes de promover a imagem;
9. preservar os locais `cloudport/migrations/navio` e `cloudport/migrations/navio-siderurgico` nos artefatos Maven;
10. restaurar backup somente como recuperação de desastre, não como rollback normal de aplicação.

## Condições que bloqueiam o corte

- checksum Flyway divergente;
- migração pendente ou falha em qualquer schema;
- branch desatualizada ou conflito com `main`;
- rota enviada simultaneamente ao monólito e ao legado;
- legado aceitando escrita durante a coexistência;
- dois jobs processando a mesma chave;
- dois deployments do Yard com consumidores RabbitMQ ativos;
- ausência de correlação entre os logs do runtime e do Yard;
- métricas operacionais indisponíveis;
- diferença não explicada nas contagens ou vínculos de dados;
- falha de autenticação, autorização ou integração com Yard;
- smoke ou testes de arquitetura falhando.
