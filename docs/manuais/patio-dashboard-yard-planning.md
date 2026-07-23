# Dashboard de Yard Planning

## Finalidade

Centralizar a leitura operacional do pátio, reunindo ocupação, ordens ativas, rejeições, candidatos a re-shuffle e os ganhos calculados pela otimização.

## Fluxo operacional

1. Acesse **Pátio > Indicadores**.
2. Use **Atualizar** para consultar novamente as APIs do Yard.
3. Confira os KPIs de ocupação, restrições, ordens e re-shuffles.
4. Compare distância e tempo originais com os valores otimizados.
5. Filtre o histórico por data ou percentual de melhoria.
6. Abra **Ver detalhes** para inspecionar entradas, resultado e rejeições.
7. Use **Exportar CSV** para analisar as leituras fora do sistema.

## Origem e fórmula das métricas

| Métrica | Origem | Fórmula |
|---|---|---|
| Ocupação | Heatmap do pátio ou posições reserváveis | posições ocupadas / total de posições × 100 |
| Posições restritas | Posições reserváveis | bloqueada, interditada ou fora de área permitida |
| Ordens ativas | Work instructions | status não final |
| Rejeições | Work instructions | status contendo indicação de rejeição |
| Re-shuffles | Análise de reshuffling | quantidade de contêineres candidatos |
| Economia de distância | Estatísticas de otimização | distância original − distância otimizada |
| Melhoria percentual | Estatísticas de otimização | economia / distância original × 100 |

O frontend apenas apresenta os valores retornados pelo backend. Quando a ocupação percentual não é publicada pelo heatmap, ela é calculada com as posições retornadas pela API.

## Histórico

Cada leitura concluída é armazenada no navegador do usuário, com limite de 50 registros. O histórico contém data da captura, entradas consultadas, resultado da otimização e rejeições identificadas. A limpeza do armazenamento do navegador remove esse histórico local.

## Campos

- **Capturado em:** horário local da leitura.
- **Ocupação:** percentual ocupado no instante da captura.
- **Ordens ativas:** instruções ainda não finalizadas.
- **Rejeições:** instruções identificadas como rejeitadas.
- **Re-shuffles:** candidatos ao reposicionamento.
- **Melhoria:** ganho percentual publicado pela otimização.

## Permissões

Usuários com permissão de leitura do pátio podem consultar o dashboard. Comandos operacionais continuam restritos aos perfis autorizados na tela de Automação, como `ADMIN_PORTO` e `PLANEJADOR`.

## Estados e bloqueios

- **Carregando:** consulta das APIs em andamento.
- **Disponível:** indicadores carregados.
- **Sem dados:** backend não retornou posições ou ordens suficientes.
- **Erro:** sessão expirada, falta de permissão ou indisponibilidade de alguma API obrigatória.

## Exemplo

Se a distância original for 1.200 e a otimizada for 900, a economia é 300 e a melhoria é 25%. A execução pode ser aberta para verificar as entradas e o resultado completo retornado pelo backend.

## Atalhos

- **Atualizar:** faz uma nova leitura e registra uma captura.
- **Exportar CSV:** baixa as leituras filtradas.
- **Ver detalhes:** abre entradas, resultado e rejeições.
