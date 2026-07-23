# Issue 728 — Previsão de demanda operacional do Yard

## Finalidade

Apoiar o planejador com uma estimativa de demanda e duração das operações, comparando a sugestão preditiva com o baseline determinístico sem permitir que o modelo persista posições diretamente.

## Fluxo operacional

1. O operador abre **Pátio > Yard Impact**.
2. Seleciona um horizonte entre 6 e 24 horas.
3. O backend consulta os planos operacionais ativos.
4. Quando o modelo está habilitado e existe amostra mínima, calcula demanda, duração e confiança.
5. Quando o modelo está desabilitado ou a amostra é insuficiente, aplica fallback determinístico integral.
6. A tela informa claramente a origem do resultado, a versão do modelo, a confiança, o baseline e as validações obrigatórias.
7. Qualquer alocação continua passando pelas regras determinísticas antes de ser persistida.

## Campos

- **Demanda prevista:** quantidade estimada de operações no horizonte.
- **Duração prevista:** tempo operacional estimado em minutos.
- **Confiança:** indicador de confiança da estimativa.
- **Baseline determinístico:** referência produzida sem modelo preditivo.
- **Diferença:** variação entre previsão e baseline.
- **Origem:** `MODELO` ou `DETERMINISTICO`.
- **Versão do modelo:** identificador rastreável do algoritmo utilizado.
- **Gerado em:** data e hora da previsão.

## Permissões

A consulta exige uma das funções `ADMIN_PORTO`, `PLANEJADOR` ou `OPERADOR_PATIO`.

## Estados possíveis

- **MODELO:** previsão preditiva habilitada e com amostra suficiente.
- **DETERMINISTICO:** fallback integral em uso.

## Motivos de bloqueio ou fallback

- modelo desabilitado por configuração;
- menos de cinco planos operacionais disponíveis;
- indisponibilidade do endpoint;
- falha nas validações determinísticas posteriores.

## Exemplos

Com 12 planos ativos e horizonte de 12 horas, o modelo pode sugerir 24 operações, exibindo o baseline de 12 e a diferença de 12. A sugestão não cria nem altera posição.

Com 3 planos ativos, a tela informa amostra insuficiente e utiliza demanda determinística igual a 3.

## Atalhos

- Atualizar projeção: botão **Atualizar projeção**.
- Ajustar horizonte: controle deslizante da seção **Horizonte da projeção**.

## Configuração

A propriedade `cloudport.yard.ml.enabled` controla o uso do modelo. O padrão é `false`, preservando o fallback determinístico até habilitação operacional explícita.

## API

`GET /api/scheduler/previsao-demanda?horizonteHoras=6`

## Processo completo

Consulte também `docs/implementados/BUS1400-BUS1410-planejamento-preditivo-patio.md` para o fluxo completo de planejamento preditivo e Yard Impact.
