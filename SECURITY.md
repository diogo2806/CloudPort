# Política de segurança do CloudPort

## Versões suportadas

O CloudPort é desenvolvido continuamente. Correções de segurança são aplicadas à branch `main` e à revisão atualmente implantada quando ela ainda divergir da `main` durante uma janela operacional de corte ou rollback.

Branches de funcionalidade, branches já mescladas e revisões históricas não recebem suporte individual. Antes de reportar uma vulnerabilidade, confirme se ela ainda é reproduzível na `main`.

| Versão ou branch | Suporte de segurança |
| --- | --- |
| `main` | suportada |
| revisão atualmente implantada | suportada durante a janela operacional vigente |
| branches de desenvolvimento ou históricas | não suportadas |

## Como reportar uma vulnerabilidade

Não publique detalhes exploráveis em uma issue, discussão, pull request, log ou captura de tela pública.

1. Acesse a aba **Security** deste repositório.
2. Use **Report a vulnerability** quando o reporte privado estiver disponível.
3. Caso a opção não esteja disponível, contate o mantenedor pelo perfil do proprietário do repositório e solicite um canal privado, sem incluir detalhes técnicos sensíveis na mensagem inicial.
4. Inclua no reporte privado:
   - componente, rota, módulo e revisão afetados;
   - pré-condições e impacto observado;
   - passos mínimos e seguros para reprodução;
   - evidências sanitizadas, sem credenciais, dados pessoais ou segredos;
   - sugestão de mitigação, quando conhecida.

Para uma indisponibilidade operacional sem risco de segurança, use uma issue comum e remova dados sensíveis.

## Triagem e tratamento

O mantenedor deve:

1. confirmar o recebimento e classificar a severidade em até 3 dias úteis;
2. validar o alcance no runtime canônico, nos módulos, no frontend e nas integrações afetadas;
3. definir mitigação imediata quando houver risco ativo;
4. preparar a correção em branch privada ou com conteúdo não explorável;
5. adicionar testes de regressão e revisar impactos em autenticação, autorização, auditoria, dados e integrações;
6. publicar a correção e, quando aplicável, um GitHub Security Advisory;
7. comunicar o repórter antes da divulgação coordenada.

Os prazos de correção dependem da severidade, da possibilidade de exploração e do risco operacional. Vulnerabilidades críticas exploráveis têm prioridade sobre melhorias funcionais.

## Segredos e credenciais

- Nunca versione senhas, tokens, chaves privadas, credenciais de banco, segredos JWT, chaves de serviço interno ou dados reais de clientes.
- Use variáveis de ambiente ou o gerenciador de segredos do ambiente de implantação.
- Valores de exemplo devem ser claramente fictícios.
- Um segredo exposto deve ser revogado e rotacionado; removê-lo do histórico não substitui a rotação.
- Logs, artefatos, mensagens EDI e evidências de erro devem ser sanitizados antes de compartilhamento.
- Pull requests devem ser revisados para impedir credenciais, permissões excessivas e dados sensíveis em testes.

## Controles e fontes oficiais

As fontes oficiais de acompanhamento de segurança são:

- **GitHub Dependabot**, para alertas de dependências;
- **GitHub CodeQL/code scanning**, para análise estática;
- **GitHub Actions**, para os workflows e evidências de validação do repositório;
- **GitHub Security Advisories**, para coordenação e divulgação de vulnerabilidades;
- esta política, para processo, escopo e responsabilidades.

Snapshots manuais de alertas não são fonte de verdade e não devem ser mantidos como relatórios permanentes no repositório.

## Requisitos para correções

Uma correção de segurança deve, sempre que aplicável:

- falhar de forma segura;
- preservar autorização no backend, sem depender apenas do frontend;
- manter idempotência, auditoria e correlação;
- evitar exposição de dados sensíveis em mensagens de erro;
- incluir teste positivo, negativo e de regressão;
- considerar compatibilidade de banco, integrações e rollback;
- registrar riscos residuais e ações posteriores como issues separadas.

## Atualização desta política

Revise este documento quando ocorrer qualquer uma destas mudanças:

- alteração da arquitetura do runtime ou dos módulos;
- criação ou retirada de um canal de reporte;
- mudança de branch suportada ou estratégia de release;
- mudança dos workflows de segurança;
- alteração do processo de implantação, corte ou rollback;
- incidente que revele lacuna no processo atual.

A revisão deve acompanhar a mesma pull request da mudança ou ser registrada imediatamente como issue de segurança documental.