# ARCH20 — identidade canônica dos planejadores de estiva

Status: implementado em 2026-07-17.

## Resultado

Os planejadores de estiva de contêineres e bobinas passaram a usar o cadastro de Navio e a VisitaNavio como fontes canônicas de identidade e contexto operacional.

## Implementação

- cadastro canônico de navio e visita operacional versionados com controle otimista;
- porta de consulta compartilhada pelos serviços standalone, runtime e monólito;
- perfis estruturais de navios graneleiros vinculados ao identificador canônico e versionados separadamente;
- planos de contêineres e bobinas persistem navio, visita, código da visita e versões das fontes utilizadas;
- validação de compatibilidade entre navio, visita e viagem;
- bloqueio de comandos quando a fonte canônica ou o perfil estrutural tiver sido alterado após a criação do plano;
- migrações Flyway aditivas e teste unitário da resolução e validação de identidade.

## Principais arquivos

- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/integracao/navio/ConsultaPlanejamentoNavioPorta.java`
- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/integracao/navio/IdentidadePlanejamentoNavioServico.java`
- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/integracao/navio/PlanejamentoCanonicoPersistenciaServico.java`
- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/vesselplanner/modelo/EstivagemPlan.java`
- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/modelo/NavioGranel.java`
- `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/estivagembulk/modelo/PlanoEstivaBulk.java`
- `backend/servico-yard/src/main/resources/db/migration/V106__identidade_canonica_planejadores_estiva.sql`
