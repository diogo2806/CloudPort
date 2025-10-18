export type ManipuladorMensagem = (corpo: string) => void;
export type ManipuladorErro = (erro: unknown) => void;

interface CabecalhosFrame {
  [chave: string]: string;
}

export class ClienteStompBasico {
  private websocket?: WebSocket;
  private conectado = false;
  private manipuladorMensagem?: ManipuladorMensagem;
  private manipuladorErro?: ManipuladorErro;
  private destinoAtual?: string;
  private idAssinatura?: string;

  constructor(private readonly url: string) {}

  conectar(destino: string, manipuladorMensagem: ManipuladorMensagem, manipuladorErro?: ManipuladorErro): void {
    this.destinoAtual = destino;
    this.manipuladorMensagem = manipuladorMensagem;
    this.manipuladorErro = manipuladorErro;
    this.idAssinatura = `sub-${Date.now()}`;

    this.websocket = new WebSocket(this.url);
    this.websocket.onopen = () => {
      this.enviarFrame('CONNECT', {
        'accept-version': '1.2',
        'heart-beat': '0,0'
      });
    };

    this.websocket.onmessage = (evento) => {
      this.processarMensagem(evento.data);
    };

    this.websocket.onerror = (evento) => {
      if (this.manipuladorErro) {
        this.manipuladorErro(evento);
      }
    };

    this.websocket.onclose = () => {
      this.conectado = false;
    };
  }

  desconectar(): void {
    if (this.websocket && this.conectado) {
      this.enviarFrame('DISCONNECT', {});
    }
    this.websocket?.close();
    this.conectado = false;
  }

  private enviarFrame(comando: string, cabecalhos: CabecalhosFrame, corpo?: string): void {
    const linhas = [comando];
    Object.entries(cabecalhos).forEach(([chave, valor]) => linhas.push(`${chave}:${valor}`));
    linhas.push('');
    const frame = `${linhas.join('\n')}${corpo ?? ''}\u0000`;
    this.websocket?.send(frame);
  }

  private processarMensagem(payload: string): void {
    const frames = payload.split('\u0000').filter((item) => item.trim().length > 0);
    frames.forEach((frame) => this.processarFrame(frame));
  }

  private processarFrame(frame: string): void {
    const linhas = frame.split('\n');
    const comando = linhas.shift()?.trim();
    if (!comando) {
      return;
    }

    const cabecalhos: CabecalhosFrame = {};
    let corpo = '';
    let linhaAtual: string | undefined;
    while ((linhaAtual = linhas.shift()) !== undefined) {
      if (linhaAtual.trim() === '') {
        corpo = linhas.join('\n');
        break;
      }
      const indiceSeparador = linhaAtual.indexOf(':');
      if (indiceSeparador > -1) {
        const chave = linhaAtual.substring(0, indiceSeparador);
        const valor = linhaAtual.substring(indiceSeparador + 1);
        cabecalhos[chave] = valor;
      }
    }

    switch (comando) {
      case 'CONNECTED':
        this.conectado = true;
        if (this.destinoAtual && this.idAssinatura) {
          this.enviarFrame('SUBSCRIBE', {
            id: this.idAssinatura,
            destination: this.destinoAtual,
            ack: 'auto'
          });
        }
        break;
      case 'MESSAGE':
        if (this.manipuladorMensagem) {
          this.manipuladorMensagem(corpo);
        }
        break;
      case 'ERROR':
        if (this.manipuladorErro) {
          this.manipuladorErro(corpo);
        }
        break;
      default:
        break;
    }
  }
}
