# Requisitos técnicos pendentes — CloudPort

Status: atualizado em 2026-07-18 após implementação dos requisitos ERR10, ERR20, ERR30, ERR40, SEC70 e SEC80.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Segurança e proteção de dados

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC90 | Exigir autenticação e autorização de assinatura nos canais WebSocket operacionais do Pátio. | Handshakes e assinaturas em `/ws/patio`, `/ws/recursos` e tópicos operacionais rejeitam usuários anônimos e origens não autorizadas; cada tópico valida perfil antes de entregar posições, contêineres, equipamentos, recursos ou eventos internos. | ⬜ Pendente |

### SEC90 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/configuracao/ConfiguracaoSeguranca.java` | regras para `/ws/patio` e `/ws/recursos` | Os endpoints de handshake e seus subcaminhos estão em `permitAll()`. Assim, a cadeia HTTP não exige JWT nem chave de serviço antes de estabelecer a sessão WebSocket. | Remover a liberação anônima e autenticar o handshake com o mecanismo canônico. Rejeitar conexão sem credencial válida antes da criação da sessão. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/configuracao/WebSocketConfiguracao.java` | `registerStompEndpoints()` e `configureMessageBroker()` | `/ws/patio`, `/ws/recursos` e `/ws/edi` aceitam `setAllowedOriginPatterns("*")`; o broker simples publica `/topico/**` e não há interceptor de entrada nem regras de autorização para `CONNECT` e `SUBSCRIBE`. | Restringir origens pela configuração CORS canônica e registrar `ChannelInterceptor` ou segurança de mensagens que valide principal e perfil em `CONNECT` e `SUBSCRIBE`. Bloquear destinos não declarados e assinaturas cruzadas entre domínios. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/patio/servico/MapaPatioServico.java` | `TOPICO_ATUALIZACOES = "/topico/patio"` e `publicarAtualizacaoTempoReal()` | O serviço publica atualizações do mapa com dados de contêineres, equipamentos, posições, movimentos e alertas no tópico operacional. Como o handshake de Pátio é público e o broker não aplica autorização por destino, clientes anônimos podem assinar o fluxo. | Autorizar a assinatura do tópico somente para perfis de leitura operacional do Pátio, sem confiar apenas na ocultação da URL pelo frontend. |
| `backend/servico-yard/src/main/java/br/com/cloudport/servicoyard/recursos/servico/ServicoRecursosService.java` | publicações em `/topico/recursos` | Eventos de recursos operacionais são enviados pelo mesmo broker sem controle de assinatura específico. | Definir permissão própria para o tópico de recursos e garantir que usuários sem esse escopo recebam `ERROR`/desconexão sem qualquer evento anterior à autorização. |
