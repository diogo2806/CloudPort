# Operação de corte e rollback do monólito modular CloudPort

Este runbook usa `backend/cloudport-runtime` como runtime canônico do CloudPort.

O executável `backend/cloudport-monolito-navio` é o primeiro corte preservado exclusivamente para rollback intermediário. Os serviços `backend/servico-*` permanecem como última camada de retorno durante a janela de compatibilidade.

## Invariantes

1. Somente um runtime aceita comandos de escrita para cada domínio.
2. Somente um runtime executa jobs agendados.
3. Somente um grupo de consumidores RabbitMQ processa cada fila operacional.
4. O `cloudport-runtime` usa portas ou eventos internos, sem HTTP entre módulos incorporados.
5. Cada schema e seu `flyway_schema_history` permanecem sob ownership do módulo correspondente.
6. Rollback troca binário e roteamento; não executa downgrade de banco.
7. Deployments, imagens e credenciais anteriores permanecem disponíveis até a paridade e o retorno serem comprovados.

## Artefatos oficiais

| Finalidade | Artefato |
| --- | --- |
| Runtime canônico | `backend/cloudport-runtime` |
| Build canônico | `backend/cloudport-modules` |
| Compose canônico | `deploy/cloudport-runtime/docker-compose.yml` |
| Rollback intermediário | `backend/cloudport-monolito-navio` |
| Compose de rollback | `deploy/navio-monolito/docker-compose.yml`, perfil `rollback` |
| Serviços isolados anteriores | `backend/servico-*` e manifests do ambiente |

## Controles de execução única

| Variável | Runtime ativo | Runtime parado ou em observação | Efeito |
| --- | --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | `false` | bloqueia comandos de escrita com `503` |
| `CLOUDPORT_JOBS_ENABLED` | `true` | `false` | habilita ou remove agendamentos |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | `false` | inicia ou impede listeners RabbitMQ |

O runtime anterior exige também `CLOUDPORT_ROLLBACK_ENABLED=true`. Sem essa propriedade, a inicialização é rejeitada.

Jobs críticos usam `pg_try_advisory_xact_lock`. Consumidores e comandos sujeitos a retry devem possuir idempotência persistente.

## Schemas e históricos

| Módulo | Schema |
| --- | --- |
| Autenticação | `cloudport_autenticacao` |
| Gate | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Visibilidade | `cloudport_visibilidade` |
| Yard | `cloudport_yard` |
| Navio | `cloudport_navio` |
| Navio Siderúrgico | `cloudport_siderurgico` |

Cada schema mantém sua própria tabela `flyway_schema_history`. Não alterar checksum, versão ou conteúdo de migração já aplicada.

## Validações antes do corte

1. Sincronizar a branch de implantação com `main` e confirmar ausência de conflitos.
2. Executar o build em `backend/cloudport-modules` e os testes de `cloudport-runtime`.
3. Validar `deploy/cloudport-runtime/docker-compose.yml` e construir a imagem canônica.
4. Executar `Flyway.validate()` nos sete schemas e confirmar ausência de migração pendente.
5. Confirmar ownership e contagens essenciais de cada domínio.
6. Criar backup consistente e registrar o ponto de restauração.
7. Confirmar que o proxy direciona cada rota para exatamente um backend.
8. Confirmar que o runtime canônico usa integrações internas em modo `local`.
9. Validar login, autorização, CORS, OpenAPI, erros, correlação, métricas e health checks.
10. Validar produção e consumo de eventos externos sem duplicação.
11. Executar smoke dos fluxos de Navio, Yard, Gate, Rail, Autenticação e Visibilidade.
12. Registrar responsável, janela, critérios de aborto e procedimento de comunicação.

## Início do runtime canônico

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

Durante a operação consolidada:

- o proxy aponta as rotas incorporadas somente para `cloudport-runtime`;
- o runtime canônico é o único escritor, scheduler e consumidor ativo;
- o `cloudport-monolito-navio` permanece parado;
- serviços isolados anteriores permanecem parados ou com escrita, jobs e consumidores desativados;
- nenhuma credencial ou imagem de rollback é removida antes do encerramento formal da janela.

## Smoke obrigatório do runtime canônico

Validar, no mínimo:

1. `health` e `prometheus`;
2. portal, Control Room e configuração dinâmica;
3. login e emissão de JWT;
4. rejeição de chamada sem token;
5. roles e restrições administrativas;
6. cadastro canônico e projeção siderúrgica por porta local;
7. mapa, reserva, ordem, work queue e work instruction do Yard;
8. Gate para Yard e Gate para Autenticação por porta local;
9. visita de trem e integração Rail/Yard;
10. projeções e alertas de Visibilidade;
11. `X-Correlation-Id` e `traceparent`;
12. erro padronizado;
13. OpenAPI sem rota ou operação duplicada;
14. persistência no schema proprietário;
15. um único job e um único consumidor por chave ou fila.

## Critérios de aprovação

O corte é aprovado quando:

- todos os smokes passam;
- não há erro de Flyway, JPA, rota duplicada ou bean duplicado;
- nenhuma chamada HTTP interna aparece nos logs do runtime canônico;
- eventos não apresentam duplicação não tratada;
- contagens e vínculos de dados permanecem consistentes;
- a operação confirma paridade dos fluxos críticos;
- o procedimento de rollback foi ensaiado no ambiente de aceitação.

## Rollback intermediário para `cloudport-monolito-navio`

O retorno não desfaz migrações. Ele reativa um binário anterior compatível com as estruturas já aplicadas.

1. Bloquear novas entradas no proxy ou colocar a aplicação em manutenção.
2. Retirar o `cloudport-runtime` do proxy.
3. Parar o `cloudport-runtime`.
4. Confirmar que não existem transações, jobs ou mensagens em processamento.
5. Validar que o banco permanece compatível com o artefato anterior.
6. Iniciar o perfil `rollback`.
7. Executar health checks e leituras controladas.
8. Direcionar as rotas para o runtime de rollback.
9. Executar uma escrita controlada em cada domínio afetado.
10. Validar auditoria, dados, filas e integrações externas.

Comando:

```bash
ROLLBACK_WRITES_ENABLED=true \
ROLLBACK_JOBS_ENABLED=true \
ROLLBACK_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile rollback \
  up -d --build
```

O perfil define `CLOUDPORT_ROLLBACK_ENABLED=true`. O executável possui adaptadores locais para `OtimizacaoYardCliente` e `PlanoOtimizadoYardCliente`, além das demais portas locais já existentes.

Nunca execute o perfil `rollback` com escrita, jobs ou consumidores ativos enquanto o `cloudport-runtime` estiver operacional.

## Rollback adicional para serviços isolados

```bash
LEGACY_NAVIO_WRITES_ENABLED=true \
LEGACY_NAVIO_JOBS_ENABLED=true \
LEGACY_NAVIO_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  up -d --build servico-navio servico-navio-siderurgico
```

Os demais serviços anteriores devem ser reativados pelos manifests atuais de cada ambiente.

## Compatibilidade Flyway

Durante a janela de retorno:

1. adicionar antes de remover;
2. manter colunas e contratos antigos enquanto o binário anterior puder voltar;
3. fazer backfill idempotente e observável;
4. evitar `DROP`, redução de tamanho e alteração incompatível de tipo;
5. evitar `NOT NULL` sem valor padrão e sem preenchimento prévio;
6. não renomear diretamente estruturas usadas pelo rollback;
7. nunca editar migração aplicada;
8. corrigir por nova versão;
9. validar os sete históricos antes da promoção;
10. encerrar formalmente a janela de rollback antes da fase destrutiva.

## Retirada de deployments e credenciais anteriores

A remoção exige:

- período de observação concluído;
- zero tráfego nos endpoints anteriores;
- zero uso de credencial interna entre módulos incorporados;
- zero consumidores e jobs anteriores ativos;
- rollback ensaiado e documentado;
- backups e imagens retidos pelo prazo definido;
- aprovação de operação e segurança.

## Condições que bloqueiam ou abortam o corte

- conflito com `main`;
- build, teste, smoke ou ArchUnit falhando;
- checksum divergente ou migração pendente;
- rota simultaneamente apontada para dois runtimes;
- mais de um escritor, scheduler ou grupo consumidor ativo;
- adaptador HTTP interno registrado no `cloudport-runtime`;
- diferença de dados sem explicação;
- falha de autenticação, autorização, CORS ou token;
- OpenAPI com operação duplicada;
- ausência de correlação, tracing ou métricas operacionais;
- incapacidade de executar o rollback ensaiado.
