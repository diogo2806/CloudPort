# Ferrovia — processo operacional completo

## Finalidade

O módulo ferroviário controla o ciclo desde o cadastro ou importação da visita até a partida do trem. O processo inclui composição física, manifesto, programação visual, ocupação das linhas, replanejamento de contêineres, inspeção de vagões, manobras, lista de trabalho e transferência de locomotivas destinadas ao navio.

## Perfis e autorização

- `ADMIN_PORTO`: administra cadastros, supervisiona e executa ações autorizadas.
- `PLANEJADOR`: cadastra visitas, importa manifestos e planeja composição, linhas e ordens.
- `OPERADOR_PATIO`: executa movimentações, inspeções e manobras conforme autorização.

Toda autorização é validada no backend. Botões ocultos ou desabilitados no frontend não substituem o controle de acesso.

## 1. Cadastro de visita e composição

Acesse **Ferrovia > Trens e composições**.

1. Informe o identificador do trem e a operadora ferroviária.
2. Defina chegada e partida previstas. A partida deve ser posterior à chegada.
3. Selecione o estado inicial da visita.
4. Adicione os vagões com identificador e tipo.
5. Ajuste a sequência com as setas para representar a ordem física.
6. Salve a visita.

### Campos

- **Identificador do trem:** código único da visita ferroviária.
- **Operadora ferroviária:** empresa responsável.
- **Chegada prevista:** instante planejado para recepção.
- **Partida prevista:** instante planejado para saída.
- **Status:** fase persistida da visita.
- **Identificador do vagão:** código do veículo na composição.
- **Tipo do vagão:** classificação operacional.
- **Posição:** ordem física no trem.

### Bloqueios

- partida igual ou anterior à chegada;
- vagão sem identificador;
- vagão repetido na composição;
- estado ou perfil incompatível;
- alteração concorrente do cadastro.

## 2. Importação do manifesto

Acesse **Ferrovia > Importar manifesto**.

1. Selecione o arquivo aceito pela integração.
2. Confirme a importação.
3. Aguarde upload, validação e persistência.
4. Revise o resultado retornado.
5. Corrija o arquivo de origem e reimporte quando houver inconsistências.

A importação pode criar ou atualizar visita, composição, vagões e operações de carga e descarga. O resultado deve ser conferido antes da execução.

### Estados da tela

- sem arquivo;
- pronto para importar;
- enviando;
- importado;
- rejeitado.

### Bloqueios

- arquivo ausente, vazio, ilegível ou em formato inválido;
- manifesto sem dados obrigatórios;
- referência duplicada ou incompatível;
- nova tentativa durante processamento ativo;
- usuário sem permissão.

## 3. Line-up e planejamento visual

Acesse **Ferrovia > Ferrovia visual**.

1. Escolha a janela de dias e a data simulada.
2. Selecione a visita.
3. Confira locomotiva, vagões, contêineres e incompatibilidades.
4. Distribua os vagões nas linhas.
5. Bloqueie um vagão quando ele não puder ser replanejado.
6. Arraste um contêiner para outro vagão e informe o motivo.
7. Revise o cronograma, as ocupações e os conflitos.

### Controles

- **Janela:** período carregado.
- **Data e hora simulada:** instante utilizado na fase calculada.
- **−6h, Agora, +6h e +24h:** navegação temporal.
- **Executar simulação:** avanço automático do instante.
- **Visita de trem:** composição exibida.
- **Linha do vagão:** posição planejada.
- **Bloquear/Desbloquear:** controla o replanejamento local.
- **Motivo:** justificativa obrigatória do movimento de contêiner.
- **Resetar plano de linhas:** retorna ao planejamento derivado da composição persistida.

### Bloqueios

- sobreposição de linha;
- vagão bloqueado;
- contêiner concluído ou ausente da composição;
- incompatibilidade de vagão;
- motivo não informado;
- versão da composição alterada por outro usuário.

## 4. Lista de trabalho

Acesse **Ferrovia > Lista de trabalho**.

1. Selecione a janela e a visita.
2. Filtre e localize a ordem.
3. Confira contêiner, operação, vagão e estado.
4. Inicie a ordem `PENDENTE`.
5. Conclua a ordem `EM_EXECUCAO` após a confirmação física.
6. Registre a partida quando a visita estiver `CONCLUIDO`.

### Estados das ordens

- `PENDENTE`;
- `EM_EXECUCAO`;
- `CONCLUIDA`.

### Estados da visita

- `PLANEJADO`;
- `CHEGOU`;
- `PROCESSANDO`;
- `CONCLUIDO`;
- `PARTIU`.

### Bloqueios

- vagão sem inspeção aprovada ou override;
- ordem sem vínculo válido no manifesto;
- tentativa de concluir uma ordem pendente;
- ordens restantes impedindo o encerramento;
- visita fora de `CONCLUIDO` ao registrar partida.

## 5. Manobras e inspeções

O painel está dentro da Ferrovia visual e possui manual próprio derivado da mesma fonte contextual das demais telas.

### Inspeção

1. Selecione o vagão.
2. Informe o responsável.
3. Verifique rodas, freios, engates, estrutura e lacres.
4. Registre observação.
5. Em caso de defeito, informe código, descrição, severidade e evidência.
6. Confirme a inspeção.

Estados:

- `APROVADA`;
- `REPROVADA`;
- `LIBERADA_OVERRIDE`.

O override exige responsável e motivo. Ele não remove a reprovação do histórico.

### Manobra

1. Informe sequência, origem, destino e composição.
2. Defina linha, trecho, início e fim previstos.
3. Reserve o trecho.
4. Resolva conflitos.
5. Autorize, inicie e conclua.

Estados:

- `PLANEJADA`;
- `BLOQUEADA_CONFLITO`;
- `AUTORIZADA`;
- `EM_EXECUCAO`;
- `CONCLUIDA`;
- `CANCELADA`.

O detalhamento técnico, as APIs e os exemplos de conflito estão em [Ferrovia — manobras e inspeções de vagões](ferrovia-manobras-inspecoes.md).

## 6. Locomotiva destinada ao navio

Uma locomotiva isolada pode ser tratada como a própria visita ferroviária e seguir para embarque como carga autopropelida.

1. Localize a visita e a locomotiva.
2. Confirme identificação, condição e custódia.
3. Vincule a transferência à visita de navio.
4. Execute o checklist e trate bloqueios.
5. Libere e conclua a movimentação ao navio.

Bloqueios comuns:

- custódia ou checklist pendente;
- visita de navio ausente;
- ordem de transferência inexistente;
- vínculo com outra operação ativa;
- estado incompatível.

## Atalhos e acessibilidade

- `F1`: abre a ajuda da rota ativa fora de campos de edição.
- `Shift + ?`: abre a ajuda da rota ativa fora de campos de edição.
- `Esc`: fecha o drawer e devolve o foco ao botão que o abriu.
- `Tab` e `Shift + Tab`: percorrem campos e ações.
- `Enter`: confirma o formulário ativo quando válido.
- O botão de ajuda possui nome acessível, `aria-expanded` e indicação de diálogo.

## Exemplo completo

Cadastre ou importe a visita `MRS-2048`, com três vagões. No line-up, distribua a composição nas linhas disponíveis. Inspecione os vagões; trate qualquer reprovação. Reserve a manobra da recepção para o pátio. Na lista de trabalho, inicie e conclua cada carga ou descarga. Quando todas as ordens estiverem concluídas, registre a partida do trem.
