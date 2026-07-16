# Operação de corte e rollback do monólito modular CloudPort

Este runbook cobre o runtime `cloudport-monolito-navio`, que incorpora Navio, Navio Siderúrgico, Yard, Gate, Rail, Autenticação e Visibilidade. O nome do diretório é histórico e pode ser alterado somente depois que pipelines e rollback deixarem de depender dele.

## Invariantes do corte

1. Somente um runtime aceita comandos de escrita para cada domínio.
2. Somente um runtime registra jobs agendados.
3. Somente um grupo de consumidores RabbitMQ processa cada fila operacional.
4. Módulos incorporados usam portas ou eventos internos, sem HTTP entre si.
5. Cada schema e seu `flyway_schema_history` permanecem sob ownership do módulo correspondente.
6. Rollback normal troca binário e roteamento; não executa downgrade de banco.
7. Deployments, imagens e credenciais legadas permanecem disponíveis até a paridade e o retorno serem comprovados.

## Controles de execução única

| Variável | Monólito ativo | Legado em coexistência | Efeito |
| --- | --- | --- | --- |
| `CLOUDPORT_WRITES_ENABLED` | `true` | `false` | bloqueia `POST`, `PUT`, `PATCH` e `DELETE` com `503` |
| `CLOUDPORT_JOBS_ENABLED` | `true` | `false` | habilita ou remove o agendamento do contexto |
| `CLOUDPORT_CONSUMERS_ENABLED` | `true` | `false` | inicia ou impede containers RabbitMQ |

Jobs críticos usam também `pg_try_advisory_xact_lock`. Consumidores e comandos sujeitos a retry devem possuir idempotência persistente.

## Schemas e históricos

| Módulo | Schema |
| --- | --- |
| Navio | `cloudport_navio` |
| Navio Siderúrgico | `cloudport_siderurgico` |
| Yard | `cloudport_yard` |
| Gate | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Autenticação | `cloudport_autenticacao` |
| Visibilidade | `cloudport_visibilidade` |

Cada schema mantém sua própria tabela `flyway_schema_history`. Não alterar checksum, versão ou conteúdo de migração já aplicada.

## Validações antes do corte

1. Sincronizar a branch de implantação com `main` e confirmar ausência de conflitos.
2. Executar o build do reator e os testes do runtime.
3. Validar o Docker Compose e construir a imagem completa.
4. Executar `Flyway.validate()` nos sete schemas e confirmar ausência de migração pendente ou falha.
5. Confirmar ownership e contagens essenciais de cada domínio.
6. Criar backup consistente e registrar o ponto de restauração.
7. Confirmar que o proxy direciona cada rota para exatamente um backend.
8. Confirmar que nenhuma configuração de produção aponta módulos incorporados para adaptadores HTTP internos.
9. Validar login, autorização, CORS, OpenAPI, erros, correlationId, métricas e health checks.
10. Validar produção e consumo de eventos externos sem duplicação.
11. Executar smoke dos fluxos de Navio, Yard, Gate, Rail, Autenticação e Visibilidade.
12. Registrar responsável, janela, critérios de aborto e procedimento de comunicação.

Consultas mínimas:

```sql
SELECT COUNT(*) FROM cloudport_navio.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_siderurgico.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_yard.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_gate.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_rail.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_autenticacao.flyway_schema_history WHERE success;
SELECT COUNT(*) FROM cloudport_visibilidade.flyway_schema_history WHERE success;
```

As consultas operacionais adicionais devem usar somente tabelas do módulo proprietário e ser registradas no plano da implantação.

## Início do monólito com coexistência controlada

```bash
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile monolito \
  --profile legado \
  up -d --build
```

Durante a coexistência:

- o proxy aponta as rotas incorporadas somente para `cloudport-monolito`;
- o monólito usa integrações locais para Navio, Yard e Autenticação;
- legados atendem no máximo leituras de comparação;
- escrita, jobs e consumidores legados permanecem desativados;
- tentativas de escrita no legado retornam `503`;
- filas não podem possuir dois grupos consumidores ativos;
- nenhuma credencial ou imagem é apagada.

## Smoke obrigatório

Validar, no mínimo:

1. `health` e `prometheus` do runtime;
2. frontend e `assets/configuracao.json`;
3. login e emissão de JWT pelo módulo incorporado de Autenticação;
4. rejeição de chamada sem token;
5. roles e restrições administrativas;
6. cadastro canônico e projeção siderúrgica por porta local;
7. consulta de mapa, reserva, ordem, work queue e work instruction do Yard;
8. consulta Gate → Yard por porta local;
9. consulta Gate → Autenticação por porta local;
10. visita de trem e integração Rail/Yard;
11. projeções e alertas de Visibilidade;
12. `X-Correlation-Id` na resposta e nos logs;
13. erro padronizado;
14. OpenAPI sem rota ou schema duplicado;
15. persistência no schema proprietário;
16. um único job e um único consumidor por chave ou fila;
17. nenhuma escrita recebida pelos legados.

## Critérios de aprovação

O corte é aprovado quando:

- todos os smokes passam;
- não há erro de Flyway, JPA, rota duplicada ou bean duplicado;
- nenhuma chamada HTTP interna aparece nos logs do runtime;
- latência, erros e consumo de recursos estão dentro da referência acordada;
- eventos não apresentam duplicação não tratada;
- contagens e vínculos de dados permanecem consistentes;
- a operação confirma paridade dos fluxos críticos;
- o procedimento de rollback foi ensaiado no ambiente de aceitação.

## Rollback da aplicação

O retorno não desfaz migrações. Ele reativa binários legados compatíveis com as estruturas já aplicadas.

1. Bloquear novas entradas no proxy ou colocar a aplicação em manutenção.
2. Retirar o monólito do proxy.
3. Parar o monólito antes de ativar qualquer escritor ou consumidor legado.
4. Confirmar que não existem transações, jobs ou mensagens em processamento.
5. Iniciar os deployments legados necessários com escrita, jobs e consumidores explicitamente habilitados.
6. Executar health checks e leituras controladas.
7. Direcionar as rotas para os deployments legados.
8. Executar uma escrita controlada em cada domínio afetado.
9. Validar auditoria, dados, filas e integrações externas.
10. Manter o monólito parado até a causa ser corrigida.

Para o perfil legado já presente no Compose:

```bash
LEGACY_NAVIO_WRITES_ENABLED=true \
LEGACY_NAVIO_JOBS_ENABLED=true \
LEGACY_NAVIO_CONSUMERS_ENABLED=true \
docker compose \
  -f deploy/navio-monolito/docker-compose.yml \
  --profile legado \
  up -d --build servico-navio servico-navio-siderurgico
```

Os demais deployments legados devem ser reativados pelos manifests atuais de cada ambiente. Nunca habilitar escritor, scheduler ou consumidor legado enquanto o monólito estiver ativo.

## Compatibilidade Flyway

Durante a janela de retorno, aplicar `expand and contract`:

1. adicionar antes de remover;
2. manter colunas e contratos antigos enquanto o binário anterior puder voltar;
3. fazer backfill idempotente e observável;
4. evitar `DROP`, redução de tamanho e alteração incompatível de tipo;
5. evitar `NOT NULL` sem valor padrão e sem preenchimento prévio;
6. não renomear diretamente tabela, coluna, índice ou constraint usada pelo legado;
7. nunca editar migração aplicada;
8. corrigir por nova versão;
9. validar os sete históricos antes da promoção;
10. preservar os locais `cloudport/migrations/<modulo>` nos artefatos;
11. encerrar formalmente a janela de rollback antes da fase destrutiva;
12. usar restauração de backup somente para recuperação de desastre.

## Retirada de deployments e credenciais legadas

A remoção ocorre em mudança separada do corte e exige:

- período de observação concluído;
- zero tráfego nos endpoints legados;
- zero uso de `X-CloudPort-Service-Key` entre módulos incorporados;
- zero consumidores e jobs legados ativos;
- rollback ensaiado e documentado;
- backups e imagens da versão anterior retidos pelo prazo definido;
- aprovação de operação e segurança;
- inventário de secrets, DNS, portas, pipelines e dashboards atualizado.

Somente depois podem ser removidos containers, manifests, imagens, credenciais, variáveis e clientes HTTP legados.

## Condições que bloqueiam ou abortam o corte

- conflito com `main`;
- build, teste, smoke ou ArchUnit falhando;
- checksum divergente ou migração pendente;
- rota simultaneamente apontada para monólito e legado;
- mais de um escritor, scheduler ou grupo consumidor ativo;
- adaptador HTTP interno registrado no runtime;
- diferença de dados sem explicação;
- falha de autenticação, autorização, CORS ou token;
- OpenAPI com operação duplicada;
- aumento não aceito de erros, filas, latência ou consumo;
- incapacidade de executar o rollback ensaiado.
