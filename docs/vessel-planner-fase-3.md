# Vessel Planner gráfico — Fase 3

A fase 3 adiciona visualizações técnicas sobre o plano persistido do navio. Os resultados do backend continuam sendo a fonte definitiva para aprovação operacional.

## Segregação IMDG gráfica

O painel identifica cargas perigosas por classe IMO, número ONU e grupo de segregação. Violações de segregação retornadas pelo backend são apresentadas como risco alto. Cargas perigosas próximas aparecem como zona gráfica de atenção, sem substituir a validação normativa do backend.

O operador pode selecionar uma carga no painel IMDG e localizar sua posição técnica no mapa e na seção do bay.

## Sequência dos guindastes

A sequência calculada é agrupada por guindaste. Cada faixa mostra:

- ordem da operação;
- contêiner ou tipo de operação;
- bay, row e tier;
- bloqueios operacionais, incluindo tampa de porão;
- seleção sincronizada com o slot correspondente.

A execução persistida planejado × realizado continua disponível abaixo do sequenciamento gráfico.

## Restow gráfico

Cada restow apresenta origem, destino, ordem, estado e motivo. Os botões de origem e destino selecionam os slots correspondentes quando eles existem na geometria do plano.

## Overlays técnicos

As camadas disponíveis são:

- segregação IMDG;
- estabilidade;
- lashing;
- força estrutural;
- risco combinado.

O mapa por stack apresenta o maior risco encontrado nos slots da pilha. A seção técnica do bay selecionado apresenta o risco exato por row e tier.

Os níveis visuais são:

- monitorado;
- atenção;
- alto ou bloqueio técnico.

O overlay combinado utiliza o maior nível entre IMDG, estabilidade, lashing e força estrutural.

## Atualização dos dados

Ao abrir um plano, o frontend consulta novamente os endpoints de estabilidade e restow. O sequenciamento gráfico utiliza o resultado produzido pelo comando “Sequenciar guindastes”. O botão “Atualizar análises” recarrega os resultados técnicos do plano.

## Limites de responsabilidade

As estimativas visuais de lashing e força estrutural usam os limites, pesos, tiers e violações disponíveis no contrato do plano. Elas auxiliam a leitura operacional, mas não substituem cálculo naval certificado, regras IMDG ou validações persistidas no backend.
