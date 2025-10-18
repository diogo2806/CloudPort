# Política de Nomenclatura e Tradução em Português do Brasil

## Objetivo
Garantir que todas as entidades de domínio, variáveis, rótulos de interface e mensagens de sistema estejam padronizadas em português do Brasil, evitando misturas com termos estrangeiros e reduzindo ambiguidades durante treinamentos e onboarding.

## Escopo
Esta política é obrigatória para todas as funcionalidades novas ou revisadas no CloudPort, abrangendo:

- Modelos de dados, entidades JPA e objetos de transferência (DTOs) no backend.
- Componentes, serviços, estados de gerenciamento, formulários e traduções no frontend.
- Documentação funcional e técnica, coleções de APIs e mensagens de integração.

## Diretrizes Gerais
1. **Termos de domínio em português**: Sempre utilize traduções oficiais aprovadas pelo time de Produto. Ex.: `Transportador`, `Lacre`, `ProtocoloDeEntrada`.
2. **Evitar abreviações estrangeiras**: Preferir `identificador` a `id`, `conteiner` a `container`, mantendo coerência com o vocabulário do setor portuário em português.
3. **CamelCase e snake_case**: Respeite o padrão adotado por cada linguagem, mas com palavras em português. Ex.: `dadosTransportador` no TypeScript, `dados_transportador` em SQL.
4. **Enumerações e constantes**: Defina valores e descrições em português. Utilize `StatusOperacao.PENDENTE` em vez de `OperationStatus.PENDING`.
5. **Rotas e endpoints**: Utilize caminhos e parâmetros em português, como `/api/transportadores/{identificador}`.
6. **Documentação**: Atualize README, ADRs e coleções de APIs sempre que novos termos forem introduzidos, garantindo alinhamento com esta política.

## Diretrizes Específicas por Camada
### Backend (Java/Spring)
- Pacotes devem refletir funcionalidades em português, ex.: `br.com.cloudport.gate.entrada`.
- Entidades devem ser nomeadas com substantivos em português e campos com termos completos (`numeroLacre`, `dataAgendamento`).
- DTOs devem permanecer no subpacote `dto` de cada funcionalidade e seguir a mesma nomenclatura das entidades.
- Validações e mensagens de erro devem utilizar `@MessageSource` com arquivos `messages_pt_BR.properties`.
- Sanitizar dados de entrada com `@Valid` e validadores customizados, retornando mensagens em português.

### Frontend (Angular)
- Componentes, serviços e variáveis devem utilizar nomes em português (`formularioCadastroTransportador.component.ts`).
- Utilize `@ngx-translate` ou equivalente com chaves em português (`formulario.campo.identificadorTransportador`).
- Nunca utilizar dados mockados: todos os rótulos dinâmicos de seleção devem vir do backend.
- Aplicar sanitização de HTML com `DomSanitizer` apenas quando estritamente necessário e revisar cada exceção.

## Processo de Revisão e Comunicação
1. **Planejamento**: Ao abrir um cartão de desenvolvimento, inclua a lista de termos do domínio que serão tocados.
2. **Desenvolvimento**: Revisar nomes durante o pair programming e confirmar traduções com o Product Owner quando houver dúvida.
3. **Code Review**: Adicionar o checklist linguístico (abaixo) em cada pull request.
4. **Documentação**: Atualizar esta política sempre que uma nova regra for estabelecida, registrando a alteração no changelog do repositório.
5. **Comunicação ao time**: Notificar o canal `#cloudport-dev` no Slack e registrar na ata da cerimônia semanal de alinhamento qualquer mudança de vocabulário.

## Checklist de Revisão Linguística (Critério de Aceite)
- [ ] Entidades e variáveis utilizam termos em português do Brasil.
- [ ] Não há termos estrangeiros nos nomes de arquivos, classes, pacotes ou rotas.
- [ ] Campos de formulários, botões e mensagens exibidas ao usuário estão em português do Brasil.
- [ ] DTOs, serviços e endpoints retornam rótulos e descrições em português.
- [ ] Documentação da funcionalidade foi atualizada com os termos revisados.
- [ ] Dados externos são sanitizados/escapados para evitar injeção de HTML.

> **Importante**: Caso algum termo precise permanecer em inglês por ser uma sigla normativa (por exemplo, `IMO`), o time de Produto deve registrar a exceção documentada nesta política.
