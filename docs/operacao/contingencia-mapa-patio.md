# Contingência do mapa operacional do pátio

## Finalidade

O modo de contingência permite consultar a última fotografia válida do mapa quando o backend ou a conexão estiver indisponível. A fotografia inclui mapa, posições, filtros, ordens, movimentações, telemetria de equipamentos e reefers, contêineres e geometrias.

A fotografia local é apenas uma referência de consulta. Ela não substitui o estado oficial persistido pelo backend.

## Estados exibidos

- `ONLINE`: a última atualização foi confirmada pelo backend.
- `RECONECTANDO`: a tela está tentando obter o estado oficial.
- `OFFLINE`: a atualização falhou ou o navegador informou perda de conectividade.

O horário da última sincronização válida permanece visível em todos os estados. Fotografias com mais de 30 minutos recebem a indicação `FOTOGRAFIA EXPIRADA`.

## Fluxo de contingência

1. Ao carregar dados online, a tela grava uma fotografia local versionada.
2. Se uma atualização falhar, a última fotografia válida continua disponível e o mapa é identificado como congelado.
3. Enquanto o estado não for `ONLINE`, movimentações, replanejamento de allocations, edição e exclusão de geometrias ficam bloqueados.
4. Nenhuma ação offline é apresentada como concluída e nenhuma fila transacional é criada no navegador.
5. Quando a conexão retorna, a tela consulta novamente todas as fontes oficiais.
6. A fotografia anterior e o estado recebido são comparados. Havendo divergência, a tela informa que o operador deve revisar posições e ordens antes de voltar a operar.

## Abertura inicial sem conexão

Se existir uma fotografia válida ou expirada, ela será exibida com a data da última sincronização. Se não houver fotografia local, a tela informa que não existem dados disponíveis e não habilita comandos.

## Ações bloqueadas

No modo `OFFLINE` ou `RECONECTANDO` são bloqueadas:

- confirmação de movimentações;
- alteração de allocations e destinos;
- criação, edição e exclusão de geometrias;
- qualquer comando que dependa de confirmação persistida pelo backend.

Operações bloqueadas devem ser registradas pelo procedimento manual do terminal e lançadas no sistema somente depois da reconciliação com o estado oficial.

## Reconciliação após retorno

A comparação usa uma impressão digital da fotografia completa. Quando houver diferença, o estado oficial substitui a fotografia local e um aviso de divergência é exibido. O operador deve revisar, no mínimo:

- posição e status dos contêineres envolvidos;
- ordens pendentes ou em execução;
- destinos planejados;
- equipamentos e telemetria;
- restrições, interdições e geometrias.

Não há reaplicação automática de comandos porque a tela não aceita comandos offline. Essa decisão evita duplicidade, conflito de posição e conclusão sem confirmação do backend.

## Permissões

As permissões normais continuam válidas. O modo de contingência somente reduz capacidades: ele nunca concede uma ação que o perfil não possua quando online.

## Atalhos e atualização

O botão `Atualizar` força uma tentativa imediata de reconexão. A atualização automática continua a cada 10 segundos. Os filtros permanecem disponíveis para consulta da fotografia carregada.
