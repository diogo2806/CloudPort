# Empresas e clientes

## Finalidade
Centralizar o cadastro de pessoas jurídicas relacionadas às operações portuárias.

## Fluxo operacional
1. Acesse **Cadastros > Empresas e clientes**.
2. Informe código, razão social, documento, país e ao menos um papel.
3. Complete contatos, endereço e observações.
4. Salve o cadastro.
5. Empresas ativas podem ser usadas nos processos; empresas inativas permanecem disponíveis apenas para histórico.

## Campos
- **Código:** identificador operacional único.
- **Razão social / nome fantasia:** identificação legal e comercial.
- **Documento:** CNPJ ou documento estrangeiro único.
- **Inscrição estadual:** registro fiscal estadual, quando aplicável.
- **Endereço e contatos:** dados para comunicação e faturamento.
- **País:** país de registro.
- **Papéis:** cliente, embarcador, consignatário, importador, exportador, dono da carga, operador, agente ou transportadora.
- **Observações:** informações complementares.

## Permissões
- `ADMIN_PORTO`: criar, alterar, ativar e inativar.
- `PLANEJADOR` e `OPERADOR_GATE`: consultar e selecionar empresas ativas.

## Estados possíveis
- **ATIVA:** disponível para novas operações.
- **INATIVA:** preservada no histórico e bloqueada para novos vínculos.

## Motivos de bloqueio
- Código já utilizado.
- CNPJ/documento já cadastrado.
- Ausência de campos obrigatórios.
- Nenhum papel selecionado.
- Usuário sem permissão de manutenção.

## Exemplos
Uma empresa pode ser simultaneamente `CLIENTE`, `IMPORTADOR` e `DONO_CARGA`. Uma companhia marítima pode ser `AGENTE` e `OPERADOR`.

## Atalhos
Use `Ctrl + K` no portal e pesquise por “empresas” ou “clientes”.

## Processo completo
Consulte também o fluxo de carga geral em `/home/carga-geral` para vincular clientes e partes da carga aos Bills of Lading.
