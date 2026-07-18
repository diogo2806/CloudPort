# Manual operacional — avarias de carga geral

## Finalidade da tela

O inspector de avarias da tela **Carga geral e break-bulk** controla o tratamento de danos identificados em um cargo lot. O fluxo registra a parcela afetada, segrega quantidade, volume e peso do saldo disponível, mantém a evidência inicial, exige inspeção e permite reintegração após reparo, baixa definitiva ou manutenção do bloqueio.

A abertura da avaria não substitui o saldo total do lote. A parcela afetada permanece contabilizada como saldo segregado e não pode ser usada em entrega, carga, transferência ou reserva enquanto estiver bloqueada.

## Fluxo operacional

1. Na grade **Inventário de cargo lots**, localize o lote e selecione **Operar**.
2. Confira os indicadores de saldo total, segregado e disponível.
3. Informe código, descrição, quantidade, volume, peso e responsável pela avaria.
4. Registre a evidência inicial com tipo, URI e, quando disponível, checksum.
5. Selecione **Registrar avaria**. O sistema bloqueia somente a parcela afetada.
6. Selecione a avaria na grade e registre o relatório de inspeção.
7. Após a inspeção, escolha um resultado:
   - `REINTEGRAR`: reparo concluído e parcela devolvida ao saldo disponível;
   - `BAIXAR`: parcela retirada definitivamente do estoque;
   - `MANTER_BLOQUEADA`: parcela continua indisponível por decisão operacional.
8. Confira o estado final, o resultado e o histórico operacional.

## Explicação dos campos

### Registro da avaria

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Código da avaria | Sim | Classificação do dano, como `EMBALAGEM_RASGADA`, `UMIDADE` ou `QUEBRA`. |
| Descrição | Sim | Relato objetivo da condição encontrada. |
| Quantidade afetada | Sim | Parcela do saldo do lote que será segregada. Deve ser maior que zero. |
| Volume afetado m³ | Sim | Volume correspondente à parcela afetada. Aceita zero quando não aplicável. |
| Peso afetado kg | Sim | Peso correspondente à parcela afetada. Aceita zero quando não aplicável. |
| Responsável | Sim | Usuário ou equipe que registrou a ocorrência. |
| Tipo da evidência | Sim | `FOTO`, `VIDEO`, `LAUDO`, `DOCUMENTO` ou `OUTRO`. |
| URI da evidência | Sim | Endereço seguro ou referência documental da evidência inicial. |
| Checksum | Não | Hash usado para comprovar a integridade do arquivo referenciado. |

### Inspeção

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Relatório de inspeção | Sim | Conclusão técnica ou operacional que fundamenta o tratamento. |
| Inspetor | Sim | Usuário responsável pela inspeção. |

### Encerramento

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Resultado do tratamento | Sim | Define reintegração, baixa ou manutenção do bloqueio. |
| Observação de encerramento | Sim | Motivo e resultado da decisão. |
| Responsável pela decisão | Sim | Usuário que autorizou o encerramento. |

## Permissões necessárias

As permissões variam por etapa:

| Etapa | Perfis autorizados |
|---|---|
| Consultar avarias | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_GATE`, `OPERADOR_PATIO` |
| Registrar avaria | `ADMIN_PORTO`, `OPERADOR_GATE`, `OPERADOR_PATIO` |
| Registrar inspeção | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO` |
| Encerrar avaria | `ADMIN_PORTO`, `PLANEJADOR` |

A autenticação e os escopos do ambiente continuam sendo aplicados antes da execução.

## Estados possíveis

| Estado | Significado | Próxima etapa |
|---|---|---|
| `ABERTA` | Ocorrência criada antes da segregação. | Segregar ou inspecionar conforme o fluxo interno. |
| `SEGREGADA` | Parcela afetada bloqueada no saldo do lote. | Registrar inspeção. |
| `EM_TRATAMENTO` | Inspeção registrada e decisão final pendente. | Reintegrar, baixar ou manter bloqueada. |
| `REINTEGRADA` | Carga reparada e parcela devolvida ao saldo disponível. | Estado final. |
| `BAIXADA` | Parcela retirada definitivamente do estoque. | Estado final. |
| `BLOQUEADA` | Parcela permanece indisponível por decisão final. | Estado final; nova decisão exige processo específico. |

## Motivos de bloqueio

A operação é rejeitada quando:

- nenhum cargo lot foi selecionado;
- o lote ou a avaria não existe;
- a quantidade afetada é zero ou negativa;
- volume ou peso são negativos;
- quantidade, volume ou peso excedem o saldo disponível;
- código, descrição, responsável ou evidência inicial não foram informados;
- o relatório de inspeção está vazio;
- há tentativa de encerramento antes da inspeção;
- o estado atual não permite a ação solicitada;
- o usuário não possui o perfil exigido;
- o endpoint simplificado legado é utilizado.

## Exemplos

### Embalagens reparadas

Um lote possui saldo total de 100 unidades e 10 embalagens danificadas.

1. Registre a avaria com quantidade afetada `10`.
2. O inspector exibirá saldo total `100`, segregado `10` e disponível `90`.
3. Registre a inspeção indicando que a embalagem pode ser substituída.
4. Após o reparo, encerre com `REINTEGRAR`.
5. O saldo segregado volta a zero e o disponível retorna para `100`.

### Perda por umidade

Um lote possui 5.000 kg, dos quais 300 kg ficaram sem condição de uso.

1. Registre a quantidade operacional, o peso afetado de `300` kg e a evidência inicial.
2. Registre o relatório de inspeção.
3. Encerre com `BAIXAR`.
4. O sistema remove a parcela bloqueada do saldo total do lote.

### Decisão pendente da seguradora

Após a inspeção, a carga não pode ser liberada nem baixada imediatamente.

1. Encerre com `MANTER_BLOQUEADA`.
2. A parcela permanece segregada e indisponível.
3. O histórico registra o responsável e a justificativa da decisão.

## Atalhos

- **F1**: abre a ajuda contextual da tela.
- **Shift + ?**: abre a ajuda contextual da tela.
- **Operar**: seleciona o cargo lot na grade.
- **Inspecionar**: abre os detalhes e o histórico da avaria.
- **Atualizar**: recarrega lotes, dashboard e saldos.
- **Manual**: abre este documento em uma nova aba.
- **Exportar**: permite exportar as grades que possuem controle de exportação.

## Processo completo e contratos

- Tela principal: [`GeneralCargoPage.jsx`](../../frontend/cloudport/src/pages/GeneralCargoPage.jsx).
- Inspector: [`GeneralCargoDamageInspector.jsx`](../../frontend/cloudport/src/pages/GeneralCargoDamageInspector.jsx).
- Serviço de domínio: [`AvariaInventarioCargaServico.java`](../../backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/servico/AvariaInventarioCargaServico.java).
- Agregado: [`AvariaOperacionalCarga.java`](../../backend/servico-carga-geral/src/main/java/br/com/cloudport/servicocargageral/dominio/AvariaOperacionalCarga.java).
- Registro: `POST /api/carga-geral/intermodal/avarias`.
- Consulta: `GET /api/carga-geral/intermodal/lotes/{loteId}/avarias`.
- Inspeção: `POST /api/carga-geral/intermodal/avarias/{id}/inspecionar`.
- Encerramento: `POST /api/carga-geral/intermodal/avarias/{id}/encerrar`.

O endpoint `POST /api/carga-geral/lotes/{id}/avarias` foi desativado e retorna `410 Gone`, pois não representa quantidade afetada, evidência, inspeção nem segregação de saldo.
