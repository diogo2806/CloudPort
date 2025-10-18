import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { CONFIGURACAO_APLICACAO_TOKEN, ConfiguracaoAplicacao } from './app/configuracao/configuracao-aplicacao.service';

async function iniciarAplicacao(): Promise<void> {
  try {
    const configuracao = await carregarConfiguracaoAplicacao();
    await platformBrowserDynamic([
      { provide: CONFIGURACAO_APLICACAO_TOKEN, useValue: configuracao }
    ]).bootstrapModule(AppModule);
  } catch (erro) {
    console.error('Falha ao iniciar a aplicação.', erro);
    exibirMensagemErro('Não foi possível iniciar a aplicação porque a configuração não pôde ser carregada. Verifique se o arquivo "assets/configuracao.json" está presente e possui o campo "baseApiUrl".');
  }
}

async function carregarConfiguracaoAplicacao(): Promise<ConfiguracaoAplicacao> {
  const resposta = await fetch('assets/configuracao.json', { cache: 'no-store' });
  if (!resposta.ok) {
    throw new Error(`Arquivo de configuração ausente ou inacessível (status ${resposta.status}).`);
  }
  let conteudo: unknown;
  try {
    conteudo = await resposta.json();
  } catch (erro) {
    throw new Error('Conteúdo do arquivo de configuração inválido. Certifique-se de que o JSON é válido.');
  }
  return validarConfiguracaoAplicacao(conteudo);
}

function validarConfiguracaoAplicacao(dados: any): ConfiguracaoAplicacao {
  const baseApiUrl = typeof dados?.baseApiUrl === 'string' ? dados.baseApiUrl.trim() : '';
  if (!baseApiUrl) {
    throw new Error('O campo "baseApiUrl" não foi informado no arquivo de configuração.');
  }
  return { baseApiUrl };
}

function exibirMensagemErro(mensagem: string): void {
  const corpo = document.body;
  if (!corpo) {
    return;
  }
  corpo.innerHTML = '';
  const container = document.createElement('div');
  container.style.display = 'flex';
  container.style.justifyContent = 'center';
  container.style.alignItems = 'center';
  container.style.height = '100vh';
  container.style.padding = '2rem';
  container.style.backgroundColor = '#0b1e39';
  container.style.color = '#ffffff';
  container.style.fontFamily = 'Arial, Helvetica, sans-serif';
  container.style.textAlign = 'center';
  container.style.fontSize = '1.25rem';
  container.style.lineHeight = '1.6';
  container.textContent = mensagem;
  corpo.appendChild(container);
}

iniciarAplicacao();
