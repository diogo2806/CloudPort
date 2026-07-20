import { createCorrelationId, request } from './api.js';

export function submitOperational2DCommand(command = {}) {
  const commandId = command.commandId ?? createCorrelationId();
  return request('/api/operacao-2d/comandos', {
    method: 'POST',
    body: {
      commandId,
      type: command.type,
      reason: command.reason ?? command.motivo ?? 'Comando confirmado no workspace gráfico 2D.',
      payload: { ...command, commandId }
    }
  });
}

export function listOperational2DWorkspaces() {
  return request('/api/operacao-2d/workspaces');
}

export function saveOperational2DWorkspace(workspace) {
  return request('/api/operacao-2d/workspaces', {
    method: 'POST',
    body: {
      name: workspace.name,
      scope: workspace.scope,
      role: workspace.role,
      content: workspace
    }
  });
}
