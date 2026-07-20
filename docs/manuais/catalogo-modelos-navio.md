# Catálogo de modelos de navio

## Finalidade da tela

A tela **Cadastros > Modelos de navio** expõe e organiza os modelos estruturais e os perfis operacionais de navios já persistidos no backend de estiva bulk.

O catálogo permite:

- consultar modelos reutilizáveis marcados como template;
- consultar perfis estruturais versionados vinculados ao cadastro canônico de navios;
- comparar identidade, dimensões, parâmetros hidrostáticos e limites estruturais;
- conferir a geometria completa de porões e setores de tanktop;
- verificar se o perfil possui dados suficientes para ser usado no planejamento de steel coils.

A tela não cria uma nova fonte de dados. Ela consome os contratos canônicos já existentes no backend e apresenta os registros em uma única visão operacional.

## Fluxo operacional

1. Acesse **Cadastros > Modelos de navio**.
2. Escolha o escopo da consulta:
   - **Todos**;
   - **Somente modelos**;
   - **Somente perfis operacionais**.
3. Pesquise pelo nome, IMO, classe ou versão dos dados técnicos.
4. Localize o registro desejado na grade.
5. Acione **Ver detalhes**.
6. Confira:
   - identidade e dimensões;
   - estabilidade, limites e versões;
   - porões;
   - setores de tanktop;
   - contrato completo retornado pelo backend.
7. No planejamento de steel coils, selecione um perfil operacional compatível com a visita e com a versão atual do cadastro canônico do navio.

## Explicação dos campos

### Identidade

- **Tipo**: diferencia `MODELO` reutilizável de `PERFIL` operacional.
- **Nome**: nome do modelo ou do navio vinculado ao perfil.
- **IMO**: código IMO do navio, quando aplicável.
- **Classe**: classificação estrutural usada pelo backend.
- **Versão do perfil**: versão sequencial do perfil estrutural.
- **Versão canônica**: versão do cadastro central do navio usada na criação do perfil.
- **ID do navio canônico**: vínculo com o cadastro principal de navios.

### Dimensões e parâmetros

- **LPP**: comprimento entre perpendiculares, em metros.
- **Boca**: largura máxima do navio, em metros.
- **Calado**: calado de referência, em metros.
- **Calado máximo**: limite operacional de calado.
- **Deslocamento**: deslocamento de referência, em toneladas.
- **GM**: altura metacêntrica usada na avaliação de estabilidade.
- **GM mínimo**: limite mínimo aceito para o perfil.
- **TPC**: toneladas por centímetro de imersão.
- **LCB**, **KM** e **MCT 1 cm**: parâmetros hidrostáticos usados nos cálculos estruturais e de estabilidade.
- **Trim máximo**: limite longitudinal de trim.
- **Banda máxima**: limite transversal de inclinação.
- **BM máximo**: limite permitido de momento fletor.
- **SF máximo**: limite permitido de esforço cortante.

### Dados de peso e versões

- **Peso leve**: peso do navio sem carga e sem consumíveis operacionais.
- **LCG, TCG e VCG do peso leve**: centros longitudinal, transversal e vertical do peso leve.
- **Peso de lastro**: peso de referência do lastro.
- **LCG, TCG e VCG do lastro**: centros do lastro.
- **Versão dos dados hidrostáticos**: identificação do conjunto de curvas e parâmetros usados.
- **Versão dos dados estruturais**: identificação do conjunto de limites de BM e SF.

### Geometria

- **Porão**: número do compartimento de carga.
- **Comprimento, largura e altura útil**: dimensões internas do porão.
- **Área útil**: área disponível para posicionamento.
- **Ângulo de antepara**: inclinação estrutural relevante ao planejamento.
- **Posição longitudinal inicial e final**: intervalo ocupado pelo porão ao longo do navio.
- **Setor**: subdivisão do tanktop dentro do porão.
- **Capacidade em t/m²**: carga máxima distribuída aceita pelo setor.
- **Área do setor**: área física disponível.
- **Limites longitudinais e transversais**: coordenadas usadas no posicionamento e na validação da carga.

## Permissões necessárias

- `ROLE_ADMIN_PORTO`: consulta completa do catálogo e acesso às funções administrativas de estiva permitidas pelo backend.
- `ROLE_PLANEJADOR`: consulta dos modelos e perfis usados no planejamento.
- Outros perfis somente acessam os endpoints quando incluídos na política `PoliticaAutorizacaoEstiva.LEITURA`.

A interface também filtra o item de navegação para os perfis administrativos e de planejamento. A autorização definitiva continua sendo aplicada pelo backend.

## Estados possíveis

- **MODELO**: registro estrutural reutilizável marcado como template.
- **PERFIL**: versão estrutural vinculada a um navio canônico.
- **Com geometria**: possui ao menos um porão exposto pelo contrato.
- **Sem geometria**: não possui porões disponíveis para inspeção.
- **Completo**: contém identidade, parâmetros técnicos, limites, porões e setores necessários ao planejamento.
- **Incompleto**: possui campos técnicos ausentes e deve ser revisado antes do uso operacional.

## Motivos de bloqueio

- sessão expirada;
- perfil autenticado sem permissão de leitura de estiva;
- endpoint indisponível ou falha na comunicação com o backend;
- modelo sem porões ou setores cadastrados;
- perfil sem vínculo com o cadastro canônico;
- perfil criado para versão anterior do navio canônico;
- parâmetros hidrostáticos ou estruturais ausentes;
- limites de estabilidade insuficientes para validar um plano;
- tentativa de usar um template diretamente onde o processo exige perfil operacional versionado.

## Exemplo

Um planejador precisa preparar a estiva de bobinas em um navio da classe `PANAMAX`.

1. Filtra o catálogo por `PANAMAX`.
2. Abre os detalhes do modelo para verificar a geometria prevista.
3. Confere a quantidade de porões e os setores de tanktop.
4. Valida capacidade em t/m², GM mínimo, BM máximo e SF máximo.
5. Localiza o perfil operacional vinculado ao navio e à versão canônica atual.
6. Usa esse perfil no planejamento de steel coils da visita correspondente.

## Atalhos

- `F1`: abre a ajuda contextual da tela.
- `Shift + ?`: abre a ajuda contextual da tela.
- `Esc`: fecha a ajuda contextual.
- **Atualizar**: recarrega modelos e perfis no backend.
- **Ver detalhes**: abre o inspector do registro selecionado.
- Campo de pesquisa: filtra por nome, IMO, classe e versões técnicas.

## Contratos do backend

### Listar perfis operacionais

```http
GET /api/estivagem-bulk/navios
```

Retorna perfis que não são templates, usando `NavioGranelDto` e incluindo porões e setores.

### Listar modelos

```http
GET /api/estivagem-bulk/navios/templates
```

Retorna modelos marcados como template, usando o mesmo contrato completo e ordenado.

### Registrar perfil estrutural

```http
POST /api/estivagem-bulk/navios
```

O registro operacional é vinculado pelo serviço de identidade ao cadastro canônico do navio e recebe uma nova versão de perfil.

## Organização técnica

O serviço `NavioGranelConsultaServico` realiza a leitura transacional e converte as entidades para DTOs. Essa conversão:

- impede a exposição direta de entidades JPA;
- evita referências bidirecionais entre navio, porão e setor;
- mantém coleções lazy dentro da transação;
- ordena navios por nome e versão;
- ordena porões pelo número;
- ordena setores pelo nome;
- entrega o número real de porões no campo `totalPoroes`.

## Processo completo

O catálogo é a etapa de preparação para o processo de planejamento de estiva de steel coils:

1. manter o cadastro canônico do navio;
2. registrar ou revisar seu perfil estrutural versionado;
3. consultar o catálogo e validar os dados técnicos;
4. selecionar navio e visita;
5. criar o plano de estiva bulk;
6. adicionar bobinas;
7. posicionar cargas nos porões e setores;
8. calcular tanktop, empilhamento, estabilidade e securing;
9. validar e aprovar o plano.

A operação de planejamento está disponível em **Navio e embarque > Steel coils** (`/home/embarque/steel-coils`).
