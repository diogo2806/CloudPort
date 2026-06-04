import { Component, OnInit } from '@angular/core';
import {
  ItemCargaSiderurgica,
  NavioSiderurgico,
  OperacaoSiderurgica,
  SiderurgicoApiService,
  TipoOperacao
} from './siderurgico-api.service';

interface ResumoOperacao {
  totalItens: number;
  pesoTotal: number;
  embarque: number;
  descarga: number;
}

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  navios: NavioSiderurgico[] = [];
  operacoes: OperacaoSiderurgica[] = [];
  itens: ItemCargaSiderurgica[] = [];
  operacaoSelecionada?: OperacaoSiderurgica;
  filtroTipo: TipoOperacao | 'TODAS' = 'TODAS';
  carregando = false;
  aviso = '';
  erro = '';

  novoNavio: NavioSiderurgico = {
    nome: '',
    codigoImo: '',
    paisBandeira: 'BR',
    empresaArmadora: '',
    tipoNavio: 'Bulk carrier',
    quantidadePoroes: 5,
    status: 'PLANEJADO'
  };

  novaOperacao: OperacaoSiderurgica = {
    navioId: 0,
    tipoOperacao: 'EMBARQUE',
    status: 'PLANEJADA',
    berco: '',
    viagem: '',
    origem: '',
    destino: ''
  };

  novoItem: ItemCargaSiderurgica = {
    codigoLote: '',
    tipoCarga: 'BOBINA',
    produto: '',
    quantidade: 1,
    pesoTotalToneladas: 0,
    status: 'PLANEJADO'
  };

  constructor(private readonly api: SiderurgicoApiService) {}

  async ngOnInit(): Promise<void> {
    await this.carregar();
  }

  get operacoesFiltradas(): OperacaoSiderurgica[] {
    return this.filtroTipo === 'TODAS'
      ? this.operacoes
      : this.operacoes.filter((operacao) => operacao.tipoOperacao === this.filtroTipo);
  }

  get resumo(): ResumoOperacao {
    const pesoTotal = this.itens.reduce((total, item) => total + Number(item.pesoTotalToneladas || 0), 0);
    return {
      totalItens: this.itens.length,
      pesoTotal,
      embarque: this.operacoes.filter((operacao) => operacao.tipoOperacao === 'EMBARQUE').length,
      descarga: this.operacoes.filter((operacao) => operacao.tipoOperacao === 'DESCARGA').length
    };
  }

  async carregar(): Promise<void> {
    this.carregando = true;
    this.erro = '';
    try {
      await this.api.carregarConfiguracao();
      this.navios = await this.api.listarNavios();
      this.operacoes = await this.api.listarOperacoes();
      if (this.navios.length && !this.novaOperacao.navioId) {
        this.novaOperacao.navioId = this.navios[0].id || 0;
      }
      if (this.operacoes.length) {
        await this.selecionarOperacao(this.operacoes[0]);
      }
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel carregar os dados do modulo siderurgico.');
    } finally {
      this.carregando = false;
    }
  }

  async criarNavio(): Promise<void> {
    this.erro = '';
    try {
      const criado = await this.api.criarNavio(this.novoNavio);
      this.navios = [...this.navios, criado];
      this.novaOperacao.navioId = criado.id || 0;
      this.novoNavio = { ...this.novoNavio, nome: '', codigoImo: '', empresaArmadora: '' };
      this.aviso = 'Navio siderurgico cadastrado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel cadastrar o navio.');
    }
  }

  async criarOperacao(): Promise<void> {
    this.erro = '';
    try {
      const criada = await this.api.criarOperacao(this.novaOperacao);
      this.operacoes = [criada, ...this.operacoes];
      await this.selecionarOperacao(criada);
      this.novaOperacao = { ...this.novaOperacao, viagem: '', berco: '', origem: '', destino: '' };
      this.aviso = 'Operacao criada.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel criar a operacao.');
    }
  }

  async selecionarOperacao(operacao: OperacaoSiderurgica): Promise<void> {
    if (!operacao.id) {
      return;
    }
    this.operacaoSelecionada = operacao;
    this.itens = await this.api.listarItens(operacao.id);
  }

  async adicionarItem(): Promise<void> {
    if (!this.operacaoSelecionada?.id) {
      this.erro = 'Selecione uma operacao antes de adicionar itens.';
      return;
    }
    try {
      const criado = await this.api.criarItem(this.operacaoSelecionada.id, this.novoItem);
      this.itens = [...this.itens, criado];
      this.novoItem = {
        codigoLote: '',
        tipoCarga: this.novoItem.tipoCarga,
        produto: '',
        quantidade: 1,
        pesoTotalToneladas: 0,
        status: 'PLANEJADO'
      };
      this.aviso = 'Item de carga registrado.';
    } catch (erro) {
      this.erro = this.extrairErro(erro, 'Nao foi possivel registrar o item.');
    }
  }

  importarCsv(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }
    const leitor = new FileReader();
    leitor.onload = () => {
      const texto = String(leitor.result || '');
      const linhas = texto.split(/\r?\n/).filter(Boolean).slice(1);
      this.itens = linhas.map((linha, indice) => {
        const [codigoLote, tipoCarga, produto, quantidade, pesoTotalToneladas, porao, origemPatio, destinoPatio] = linha.split(',');
        return {
          codigoLote: codigoLote || `LOTE-${indice + 1}`,
          tipoCarga: (tipoCarga || 'BOBINA') as ItemCargaSiderurgica['tipoCarga'],
          produto: produto || 'Produto siderurgico',
          quantidade: Number(quantidade || 1),
          pesoTotalToneladas: Number(pesoTotalToneladas || 0),
          porao: porao ? Number(porao) : null,
          origemPatio,
          destinoPatio,
          sequenciaOperacional: indice + 1,
          status: 'PLANEJADO'
        };
      });
      this.aviso = 'Planilha CSV carregada localmente. Registre os itens na API conforme a operacao.';
      input.value = '';
    };
    leitor.readAsText(arquivo);
  }

  exportarCsv(): void {
    const cabecalho = 'codigo_lote,tipo_carga,produto,quantidade,peso_total_toneladas,porao,origem_patio,destino_patio';
    const linhas = this.itens.map((item) => [
      item.codigoLote,
      item.tipoCarga,
      item.produto,
      item.quantidade,
      item.pesoTotalToneladas,
      item.porao || '',
      item.origemPatio || '',
      item.destinoPatio || ''
    ].join(','));
    const blob = new Blob([[cabecalho, ...linhas].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'operacao_siderurgica.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  private extrairErro(erro: unknown, fallback: string): string {
    const resposta = erro as { error?: { erro?: string; message?: string } };
    return resposta.error?.erro || resposta.error?.message || fallback;
  }
}
