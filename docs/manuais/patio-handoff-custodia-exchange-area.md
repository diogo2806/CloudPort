# Handoff de custódia em exchange area

## Finalidade da tela

O painel registra a transferência bilateral de custódia de uma unidade em uma exchange area do pátio. A entrega e o recebimento mantêm unidade, área, posição física, equipamento, operador, condição, lacres e instante. A custódia só muda uma vez e qualquer divergência física gera bloqueio operacional.

## Fluxo operacional

1. Acesse **Pátio > Indicadores**.
2. No painel **Entregar na exchange area**, informe a unidade e o local físico.
3. Informe o equipamento, o operador, a condição e todos os lacres conferidos.
4. Confirme a entrega. O registro passa para **ENTREGUE** e aguarda a segunda conferência.
5. Na grade de custódias, selecione **Receber**.
6. O operador de destino repete a leitura física, sem copiar automaticamente equipamento e responsável da entrega.
7. Confirme o recebimento.
8. Se unidade, área, posição, condição e lacres forem equivalentes, o estado passa para **RECEBIDA**.
9. Se qualquer campo físico divergir, o estado passa para **DIVERGENTE**, a custódia permanece bloqueada e o motivo fica visível no painel.

## Explicação dos campos

- **Unidade:** código físico do contêiner ou unidade de carga.
- **Exchange area:** área operacional em que ocorre a troca de custódia.
- **Posição física:** slot, vaga ou referência exata dentro da área.
- **Equipamento:** CHE responsável pela entrega ou pelo recebimento.
- **Operador:** pessoa que confirma cada lado do handoff.
- **Condição:** estado físico observado, como íntegro, avariado ou com ressalva.
- **Lacres:** conjunto de lacres conferidos. O backend normaliza, remove repetições e compara sem depender da ordem digitada.
- **Instante da entrega:** horário persistido na primeira confirmação.
- **Instante do recebimento:** horário persistido na segunda confirmação.
- **Motivo da divergência:** memória de cálculo com cada campo físico incompatível.
- **Chave idempotente:** identificador técnico preservado pelo portal durante retentativas para impedir execução duplicada.

## Permissões necessárias

- **ADMIN_PORTO:** consulta, entrega e recebimento.
- **OPERADOR_PATIO:** consulta, entrega e recebimento.
- **PLANEJADOR:** consulta do painel e dos estados persistidos.

## Estados possíveis

- **ENTREGUE:** a primeira confirmação foi persistida e a custódia aguarda recebimento.
- **RECEBIDA:** os dois lados conferiram os mesmos dados físicos e a transferência foi concluída uma única vez.
- **DIVERGENTE:** a segunda conferência encontrou incompatibilidade e bloqueou a custódia.

## Motivos de bloqueio

- Já existe custódia **ENTREGUE** ou **DIVERGENTE** para a mesma unidade.
- A custódia já foi recebida e uma nova tentativa pretende alterar o resultado.
- A custódia já está divergente e não pode receber outra confirmação.
- A chave idempotente foi reutilizada com conteúdo diferente.
- Unidade divergente.
- Exchange area divergente.
- Posição física divergente.
- Condição divergente.
- Conjunto de lacres divergente.
- Campo obrigatório ausente.
- Perfil sem permissão operacional.

## Exemplos

### Transferência concluída

A unidade `CONT-001` é entregue na área `EA-01`, posição `P-03`, em condição `ÍNTEGRO`, com os lacres `L10` e `L20`. No recebimento, o segundo operador informa os mesmos dados e digita `L20, L10`. A normalização identifica o mesmo conjunto de lacres e conclui a custódia como **RECEBIDA**.

### Divergência de condição

A entrega registra a unidade como `ÍNTEGRO`. O recebimento informa `AVARIADO`. O sistema persiste os dados da segunda leitura, altera o estado para **DIVERGENTE**, informa a condição esperada e recebida e mantém o handoff bloqueado.

### Retentativa de comunicação

O navegador envia a entrega e perde a resposta por falha de rede. Ao repetir o comando com a mesma chave idempotente e os mesmos dados, o backend devolve o registro existente sem criar outra custódia.

## Atalhos

- **F1:** abrir a ajuda contextual da tela.
- **Shift + ?:** abrir a ajuda contextual da tela.
- **Esc:** fechar a ajuda contextual.
- **Atualizar:** recarregar as custódias persistidas.
- **Receber:** selecionar uma entrega pendente e abrir a segunda conferência.
- **Inspecionar:** visualizar um registro recebido ou divergente.

## Processo completo

O processo está disponível em **Pátio > Indicadores**, na rota `/home/patio/dashboard-kpi`. A API correspondente usa:

- `GET /yard/patio/exchange-areas/custodias`
- `POST /yard/patio/exchange-areas/custodias/entregas`
- `POST /yard/patio/exchange-areas/custodias/{custodiaId}/recebimentos`

A persistência usa bloqueio pessimista no recebimento, versão otimista, unicidade de chaves idempotentes e uma restrição parcial que permite somente uma custódia ativa por unidade.
