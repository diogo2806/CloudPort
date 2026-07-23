# Segurança técnica do runtime

A política canônica de suporte, reporte responsável, triagem, segredos e divulgação está em [`../SECURITY.md`](../SECURITY.md). Este documento descreve somente o contexto técnico do runtime e não substitui a política.

## Superfícies principais

O `backend/cloudport-runtime` concentra autenticação, autorização, CORS, OpenAPI e configuração transversal dos módulos Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

As integrações externas incluem TOS, OCR, EDI, RabbitMQ, Redis, storage e clientes HTTP autorizados na borda. Cada integração deve usar credenciais próprias, escopo mínimo, timeout, correlação, sanitização de logs e comportamento fail-closed quando a operação exigir segurança ou consistência.

## Regras técnicas

- Autorizações mutáveis devem ser validadas no backend.
- O frontend pode ocultar ações, mas não constitui controle de acesso.
- Segredos são fornecidos por ambiente e nunca mantidos no código-fonte.
- Erros não devem retornar credenciais, tokens, dados pessoais nem mensagens brutas de parceiros.
- Comandos operacionais críticos devem preservar idempotência, auditoria e correlation ID.
- Migrations e correções devem respeitar compatibilidade de corte e rollback.
- Testes de segurança devem cobrir acesso permitido, acesso negado e regressão da falha corrigida.

## Acompanhamento

Dependabot, CodeQL, GitHub Actions e GitHub Security Advisories são as fontes oficiais de acompanhamento. O fluxo de reporte, classificação e divulgação permanece definido exclusivamente em [`SECURITY.md`](../SECURITY.md).