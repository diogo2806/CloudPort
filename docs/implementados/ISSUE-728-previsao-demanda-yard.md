# Issue 728 — Previsão de demanda operacional do Yard

## Finalidade
Apoiar o planejador com estimativas de demanda e duração, comparadas ao baseline determinístico, sem permitir persistência direta de posições pelo modelo.

## Fluxo operacional
1. Acessar **Pátio > Yard Impact**.
2. Selecionar horizonte entre 6 e 24 horas.
3. Consultar previsão, duração, confiança e baseline.
4. Verificar se a origem é `MODELO` ou `DETERMINISTICO`.
5. Manter todas as alocações sujeitas às validações determinísticas.

## Campos
- Demanda prevista.
- Duração prevista.
- Confiança.
- Baseline determinístico e diferença.
- Origem e versão do modelo.
- Data de geração.

## Permissões
`ADMIN_PORTO`, `PLANEJADOR` ou `OPERADOR_PATIO`.

## Estados possíveis
- `MODELO`: modelo habilitado e amostra suficiente.
- `DETERMINISTICO`: fallback integral.

## Motivos de bloqueio ou fallback
- modelo desabilitado;
- menos de cinco planos operacionais;
- indisponibilidade do endpoint;
- reprovação nas regras determinísticas posteriores.

## Exemplos
Com 12 planos e horizonte de 12 horas, a previsão pode indicar 24 operações, baseline 12 e diferença 12. Nenhuma posição é criada automaticamente.

Com 3 planos, o fallback usa demanda determinística igual a 3.

## Atalhos
- **Atualizar projeção** recalcula os dados.
- O controle deslizante altera o horizonte.

## Configuração
`cloudport.yard.ml.enabled=false` por padrão.

## API
`GET /api/scheduler/previsao-demanda?horizonteHoras=6`

## Processo completo
Consulte `docs/implementados/BUS1400-BUS1410-planejamento-preditivo-patio.md`.
