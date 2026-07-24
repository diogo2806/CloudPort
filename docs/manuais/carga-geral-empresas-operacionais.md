# Manual — Carga geral e empresas operacionais

## Finalidade da tela

A tela **Carga geral e break-bulk** controla Bills of Lading, itens, cargo lots, estoque, movimentações parciais, avarias e referências. A integração com o cadastro mestre de empresas evita nomes livres e mantém cada participante vinculado por identificador, papel e situação cadastral.

## Fluxo operacional

1. Cadastre ou revise a empresa em **Cadastros > Empresas**.
2. Garanta que a empresa esteja ativa e possua os papéis necessários.
3. Abra **Carga > Carga geral e break-bulk**.
4. No novo Bill of Lading, informe número, operação, embarcador e consignatário; selecione os demais participantes aplicáveis.
5. Crie o Bill of Lading. O sistema registra os nomes como fotografia operacional e persiste os vínculos pelos identificadores das empresas.
6. Selecione o Bill na grade e use **Empresas do Bill of Lading** para consultar, corrigir, incluir ou remover vínculos.
7. Cadastre o item e crie o cargo lot, selecionando cliente, dono da carga, operador e transportadora quando aplicáveis.
8. Selecione o cargo lot e use **Empresas do cargo lot** para manter seus vínculos.
9. Continue o fluxo de recebimento, descarga, armazenagem, transferência, carga, entrega, consolidação ou desconsolidação.

Toda inclusão, alteração ou remoção de vínculo gera auditoria com recurso, papel, empresa anterior, empresa nova, usuário e data.

## Explicação dos campos

### Bill of Lading

- **Número**: identificador único do conhecimento.
- **Operação**: importação, exportação, cabotagem ou transbordo.
- **Embarcador**: empresa ativa com papel `EMBARCADOR`; obrigatório.
- **Consignatário**: empresa ativa com papel `CONSIGNATARIO`; obrigatório.
- **Cliente**: empresa contratante com papel `CLIENTE`.
- **Importador**: empresa com papel `IMPORTADOR`.
- **Exportador**: empresa com papel `EXPORTADOR`.
- **Dono da carga**: proprietário comercial com papel `DONO_CARGA`.
- **Operador**: responsável operacional com papel `OPERADOR`.
- **Agente**: representante com papel `AGENTE`.
- **Transportadora**: empresa rodoviária ou logística com papel `TRANSPORTADORA`.
- **Visita do navio**, **visita do veículo** e **armazém**: identificadores dos recursos operacionais relacionados.
- **Portos de origem e destino**: referências logísticas do conhecimento.

### Cargo lot

- **Código, natureza, quantidade, volume, peso e unidade**: identidade e previsão física do lote.
- **Cliente, dono da carga, operador e transportadora**: vínculos empresariais aplicáveis ao lote.
- **Armazém e posição**: destino físico atual ou planejado.
- **Veículo e visita do navio**: recursos de transporte relacionados.
- **Lote pai**: lote de origem em cenários de desconsolidação.

### Painéis de empresas

- Cada seletor mostra apenas empresas ativas e compatíveis com o papel.
- Um vínculo antigo que se tornou inativo ou incompatível permanece visível com indicação de bloqueio, permitindo sua substituição ou remoção.
- **Salvar empresas** substitui a composição completa de vínculos do recurso selecionado.

## Permissões necessárias

- `ADMIN_PORTO`: consultar cadastros, criar recursos e manter vínculos.
- `PLANEJADOR`: consultar cadastros, criar Bills/cargo lots e manter vínculos.
- `OPERADOR_GATE`: consultar carga geral e vínculos, sem permissão para substituir empresas.

A manutenção do cadastro mestre, incluindo ativação, inativação e alteração de papéis, é exclusiva de `ADMIN_PORTO`.

## Estados possíveis

- **Carregando**: catálogo ou vínculos estão sendo consultados.
- **Sem empresas disponíveis**: não há cadastro acessível ou a consulta foi negada.
- **Sem empresa compatível**: não existe empresa ativa com o papel do campo.
- **Vínculo ativo**: empresa ativa e com o papel correspondente.
- **Vínculo bloqueado**: empresa inativa ou sem o papel requerido.
- **Salvando**: atualização em processamento.
- **Sucesso**: vínculos persistidos e auditados.
- **Erro**: operação rejeitada; a mensagem apresenta o motivo retornado pelo backend.

## Motivos de bloqueio

- Empresa inexistente.
- Empresa inativa.
- Empresa sem o papel solicitado.
- Papel não aplicável ao tipo de recurso.
- Mesmo papel informado mais de uma vez.
- Bill of Lading ou cargo lot não encontrado.
- Usuário sem permissão de alteração.
- Cadastro mestre de empresas indisponível.

## Exemplos

### Importação

- Embarcador: `SID-EXPORT` com papel `EMBARCADOR`.
- Consignatário: `IND-BR` com papel `CONSIGNATARIO`.
- Importador: `IND-BR` com papel `IMPORTADOR`.
- Operador: `OPER-TERM` com papel `OPERADOR`.
- Transportadora do cargo lot: `TRANS-001` com papel `TRANSPORTADORA`.

### Substituição por inativação

Se `TRANS-001` estiver inativa, o vínculo atual aparece como bloqueado. Selecione outra empresa ativa com papel `TRANSPORTADORA` e salve. A auditoria registra `TRANS-001` como empresa anterior e a nova empresa como substituta.

## Atalhos

- Use **Selecionar** na grade de Bills para abrir itens e empresas do conhecimento.
- Use **Usar no lote** para associar rapidamente o item ao próximo cargo lot.
- Use **Operar** na grade de cargo lots para abrir empresas, movimentação e avarias.
- Use **Atualizar** no cabeçalho após alterações externas no cadastro de empresas.
- Use o ícone **? Manual** para reabrir este documento.

## Processo completo

Consulte o [requisito técnico e fluxo completo do CloudPort](../requisitos/requisito-tecnico.md) e o [manual de descarga de amarrados mistos](carga-geral-amarrados-mistos.md) para os passos posteriores de direcionamento físico.
