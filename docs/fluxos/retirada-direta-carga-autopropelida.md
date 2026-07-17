# Retirada direta de carga autopropelida do navio

## Cenário operacional

Uma carga identificada e autopropelida, como um trator, é descarregada do navio. O cliente autorizado assume a condução do próprio equipamento e sai diretamente pelo gate.

Esse cenário não representa uma visita normal de caminhão. Portanto, o CloudPort não cria uma entrada fictícia, não exige placa de cavalo mecânico e não passa pelo fluxo comum de agendamento de veículo.

## Fluxo no CloudPort

1. A operação do navio confirma que o equipamento foi descarregado.
2. A documentação e a liberação aduaneira são conferidas.
3. O gate confirma a identidade e a habilitação do cliente que conduzirá o equipamento.
4. O operador informa a autorização de retirada, o identificador da carga e a visita do navio.
5. O CloudPort registra a saída definitiva e mantém o histórico auditável.

A funcionalidade está disponível no portal em **Gate > Console do operador**, na tela **Saída direta do navio**.

## Regras obrigatórias

A saída somente é registrada quando todas as condições abaixo forem confirmadas:

- documentação da carga validada;
- liberação aduaneira confirmada;
- descarga do navio confirmada;
- condutor habilitado para operar o equipamento.

O código da autorização e o identificador da carga são únicos, sem diferenciação entre letras maiúsculas e minúsculas. Uma repetição com a mesma autorização e a mesma carga é tratada de forma idempotente. Uma autorização reutilizada para outra carga ou uma carga já retirada é rejeitada.

## API

### Registrar saída

`POST /gate/retiradas-diretas-navio`

```json
{
  "codigoAutorizacao": "AUT-2026-001",
  "identificadorCarga": "TRATOR-CHASSI-001",
  "tipoCarga": "TRATOR",
  "visitaNavio": "VV-2026-009",
  "clienteNome": "Cliente autorizado",
  "clienteDocumento": "12345678900",
  "documentosValidados": true,
  "liberacaoAduaneiraConfirmada": true,
  "cargaDescarregada": true,
  "condutorHabilitado": true,
  "observacao": "Retirada direta após descarga"
}
```

Perfis autorizados: `ADMIN_PORTO` e `OPERADOR_GATE`.

### Consultar histórico

`GET /gate/retiradas-diretas-navio?page=0&size=50`

Perfis autorizados: `ADMIN_PORTO`, `OPERADOR_GATE` e `PLANEJADOR`.

## Persistência e auditoria

Cada saída registra:

- autorização;
- identificador e tipo da carga;
- visita do navio;
- cliente e documento;
- horário efetivo da saída;
- operador autenticado;
- observação;
- status final.

A tabela pertence ao schema do módulo Gate e não compartilha entidades ou repositórios com os módulos Navio e Yard.

## Relação com o processo portuário

O trator é tratado como carga break-bulk identificada, pois possui identificador próprio. A retirada é direta entre o fluxo de descarga do navio e o gate, sem a etapa de armazenamento no pátio e sem transformar o próprio trator em caminhão de transporte do gate.
