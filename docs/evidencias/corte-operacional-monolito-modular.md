# Prova do corte operacional do monólito modular

## Escopo

A prova é executada em ambiente efêmero de aceitação por `deploy/cloudport-runtime/provar-corte.sh`. Ela não promove automaticamente uma versão no EasyPanel e não remove deployments, imagens ou credenciais de produção. Essas ações continuam dependentes da janela e da governança do ambiente real.

## Controles implementados

1. `CLOUDPORT_WRITES_ENABLED`, `CLOUDPORT_JOBS_ENABLED` e `CLOUDPORT_CONSUMERS_ENABLED` iniciam desabilitados quando ausentes.
2. Somente a instância canônica recebe os três controles habilitados.
3. Uma instância observadora usa o mesmo banco com escrita, jobs e consumidores desabilitados.
4. Comandos HTTP de escrita na instância observadora retornam `503` e `RUNTIME_SOMENTE_LEITURA`.
5. `GET /operacao/corte` publica o papel da instância, a revisão, os controles ativos, os adaptadores locais e os schemas incorporados.

## Provas executadas

O script executa e registra:

1. uma única instância escritora do `cloudport-runtime`;
2. jobs e consumidores habilitados somente no runtime canônico;
3. consumidores RabbitMQ com concorrência unitária no ambiente da prova;
4. health, readiness, Prometheus e autenticação fail-closed;
5. oito schemas com histórico Flyway aplicado;
6. superfície OpenAPI dos módulos Autenticação, Carga Geral, Gate, Rail, Yard, Navio e Navio Siderúrgico;
7. adaptadores internos de Autenticação, Navio e Yard em modo local;
8. Redis e RabbitMQ disponíveis;
9. TOS em sucesso, resposta inválida, timeout e indisponibilidade;
10. contratos automatizados de TOS, OCR e EDI;
11. escrita e leitura funcional no runtime canônico;
12. persistência do banco e do volume de documentos após reinício;
13. interrupção do escritor canônico antes do runtime anterior;
14. leitura pelo runtime de rollback sobre o mesmo banco sem downgrade;
15. bloqueio de escrita durante o rollback;
16. retorno ao runtime canônico e nova leitura do dado criado antes do ensaio.

## Execução

Na raiz do repositório:

```bash
bash deploy/cloudport-runtime/provar-corte.sh
```

O workflow `.github/workflows/validate-cloudport.yml` executa o mesmo comando no job `Provar corte e rollback do monólito modular`.

## Evidências

O diretório padrão é `/tmp/cloudport-cutover-evidence`. O workflow publica esse diretório como artefato `evidencia-corte-operacional`.

Arquivos principais:

- `evidencia-corte-operacional.json`;
- `evidencia-corte-operacional.md`;
- `canonical-status.json`;
- `observer-status.json`;
- `flyway-counts.txt`;
- `rabbit-consumers.txt`;
- `openapi.json`;
- `compose-final.txt`;
- `compose.log`.

## Critério para produção

A promoção no EasyPanel deve usar a mesma revisão comprovada e repetir os seguintes controles:

1. confirmar somente uma instância com escrita, jobs e consumidores ativos;
2. manter runtimes anteriores sem escrita, jobs e consumidores;
3. validar readiness antes da troca de rota;
4. observar métricas, filas e logs durante a janela acordada;
5. não remover o runtime anterior até concluir o período de observação;
6. em rollback, interromper o runtime canônico antes de habilitar qualquer escrita no runtime anterior.
