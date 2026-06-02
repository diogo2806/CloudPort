import { Component, OnInit } from '@angular/core';
import { ServicoAutomacaoPatio } from '../../service/servico-automacao-patio/servico-automacao-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

interface Automacao {
  totalConteineresPlanificados: number;
  totalConteineresSucesso: number;
  totalConteineresFalha: number;
  percentualSucesso: number;
  containersPlanificados: string[];
  containersException: string[];
  mensagemResumo: string;
  temExcecoes: boolean;
}

@Component({
  selector: 'app-automacao-tarefas-patio',
  templateUrl: './automacao-tarefas-patio.component.html',
  styleUrls: ['./automacao-tarefas-patio.component.css'],
  standalone: false
})
export class AutomacaoTarefasPatioComponent implements OnInit {
  resultado: Automacao | null = null;
  carregando = false;
  erro?: string;
  historicoExecucoes: Automacao[] = [];
  mostraDetalhes = false;

  constructor(
    private readonly servicoAutomacao: ServicoAutomacaoPatio,
    private readonly sanitizador: SanitizadorConteudoService
  ) {}

  ngOnInit(): void {
    this.carregarHistorico();
  }

  executarAutoplanejamento(): void {
    if (this.carregando) {
      return;
    }

    this.carregando = true;
    this.erro = undefined;

    this.servicoAutomacao.executarAutoplanejamento().subscribe({
      next: (resposta) => {
        this.resultado = resposta;
        this.historicoExecucoes.unshift({ ...resposta });
        if (this.historicoExecucoes.length > 5) {
          this.historicoExecucoes.pop();
        }
        this.carregando = false;
        this.salvarHistorico();
      },
      error: (erro) => {
        this.erro = `Erro ao executar automação: ${erro?.error?.message || 'falha desconhecida'}`;
        this.carregando = false;
      }
    });
  }

  obterCorSucesso(percentual: number): string {
    if (percentual === 100) {
      return '#10b981';
    } else if (percentual >= 80) {
      return '#f59e0b';
    } else {
      return '#dc2626';
    }
  }

  obterIconeSucesso(percentual: number): string {
    if (percentual === 100) {
      return '✅';
    } else if (percentual >= 80) {
      return '⚠️';
    } else {
      return '❌';
    }
  }

  obterDescricaoStatus(percentual: number): string {
    if (percentual === 100) {
      return 'Automação 100% bem-sucedida';
    } else if (percentual >= 80) {
      return 'Automação com sucesso parcial';
    } else {
      return 'Automação com muitas exceções';
    }
  }

  private carregarHistorico(): void {
    const historicoSalvo = localStorage.getItem('historicoAutomacao');
    if (historicoSalvo) {
      try {
        this.historicoExecucoes = JSON.parse(historicoSalvo);
      } catch (e) {
        this.historicoExecucoes = [];
      }
    }
  }

  private salvarHistorico(): void {
    localStorage.setItem('historicoAutomacao', JSON.stringify(this.historicoExecucoes));
  }

  alternaoDetalhes(): void {
    this.mostraDetalhes = !this.mostraDetalhes;
  }

  sanitizar(texto: string | null | undefined): string {
    return this.sanitizador.sanitizar(texto ?? '');
  }

  limparHistorico(): void {
    if (confirm('Tem certeza que deseja limpar o histórico?')) {
      this.historicoExecucoes = [];
      localStorage.removeItem('historicoAutomacao');
    }
  }
}
