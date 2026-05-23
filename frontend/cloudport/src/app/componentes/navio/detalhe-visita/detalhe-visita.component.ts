import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import {
  Berco,
  OperacaoNavioConteiner,
  OperacaoRequest,
  PlanejamentoAtracacao,
  ServicoNavioService,
  StatusOperacaoNavioConteiner,
  VisitaNavioDetalhe
} from '../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-navio-detalhe-visita',
  templateUrl: './detalhe-visita.component.html',
  standalone: false
})
export class DetalheVisitaComponent implements OnInit {
  visita: VisitaNavioDetalhe | null = null;
  bercos: Berco[] = [];
  carregando = false;
  erro: string | null = null;
  mensagem: string | null = null;

  planejamento: PlanejamentoAtracacao = { bercoId: 0, atracacaoPrevista: '', desatracacaoPrevista: '' };
  novaOperacao: OperacaoRequest = this.operacaoVazia();

  constructor(
    private readonly servico: ServicoNavioService,
    private readonly rota: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const id = Number(this.rota.snapshot.paramMap.get('id'));
    if (!id) {
      this.erro = 'Visita inválida.';
      return;
    }
    this.servico.listarBercos().subscribe({
      next: (dados) => (this.bercos = dados),
      error: () => undefined
    });
    this.carregar(id);
  }

  carregar(id: number): void {
    this.carregando = true;
    this.erro = null;
    this.servico.obterVisita(id).subscribe({
      next: (dados) => {
        this.aplicar(dados);
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar a visita.';
        this.carregando = false;
      }
    });
  }

  planejar(): void {
    if (!this.visita || !this.planejamento.bercoId || !this.planejamento.atracacaoPrevista || !this.planejamento.desatracacaoPrevista) {
      this.erro = 'Selecione o berço e a janela de atracação.';
      return;
    }
    const payload: PlanejamentoAtracacao = {
      bercoId: this.planejamento.bercoId,
      atracacaoPrevista: this.comSegundos(this.planejamento.atracacaoPrevista),
      desatracacaoPrevista: this.comSegundos(this.planejamento.desatracacaoPrevista)
    };
    this.executar(this.servico.planejarAtracacao(this.visita.identificador, payload), 'Atracação planejada.');
  }

  confirmarChegada(): void {
    if (this.visita) {
      this.executar(this.servico.registrarChegada(this.visita.identificador), 'Chegada confirmada.');
    }
  }

  atracar(): void {
    if (this.visita) {
      this.executar(this.servico.atracar(this.visita.identificador), 'Navio atracado.');
    }
  }

  iniciarOperacaoVisita(): void {
    if (this.visita) {
      this.executar(this.servico.iniciarOperacao(this.visita.identificador), 'Operação iniciada.');
    }
  }

  concluirOperacaoVisita(): void {
    if (this.visita) {
      this.executar(this.servico.concluirOperacao(this.visita.identificador), 'Operação concluída.');
    }
  }

  desatracar(): void {
    if (this.visita) {
      this.executar(this.servico.desatracar(this.visita.identificador), 'Navio desatracado.');
    }
  }

  cancelar(): void {
    if (this.visita) {
      this.executar(this.servico.cancelar(this.visita.identificador), 'Visita cancelada.');
    }
  }

  adicionarOperacao(): void {
    if (!this.visita || !this.novaOperacao.identificacaoConteiner) {
      this.erro = 'Informe a identificação do contêiner.';
      return;
    }
    this.servico.adicionarOperacao(this.visita.identificador, this.novaOperacao).subscribe({
      next: () => {
        this.novaOperacao = this.operacaoVazia();
        this.carregar(this.visita!.identificador);
      },
      error: () => (this.erro = 'Não foi possível adicionar a operação.')
    });
  }

  alterarStatusOperacao(operacao: OperacaoNavioConteiner, status: StatusOperacaoNavioConteiner): void {
    if (!this.visita) {
      return;
    }
    this.servico.atualizarStatusOperacao(this.visita.identificador, operacao.identificador, status).subscribe({
      next: () => this.carregar(this.visita!.identificador),
      error: () => (this.erro = 'Não foi possível atualizar a operação.')
    });
  }

  removerOperacao(operacao: OperacaoNavioConteiner): void {
    if (!this.visita) {
      return;
    }
    this.servico.removerOperacao(this.visita.identificador, operacao.identificador).subscribe({
      next: () => this.carregar(this.visita!.identificador),
      error: () => (this.erro = 'Não foi possível remover a operação.')
    });
  }

  voltar(): void {
    this.router.navigate(['/home/navio/visitas']);
  }

  posicaoEstiva(operacao: OperacaoNavioConteiner): string {
    if (operacao.bay == null && operacao.fileira == null && operacao.altura == null) {
      return '—';
    }
    return `${operacao.bay ?? '?'} / ${operacao.fileira ?? '?'} / ${operacao.altura ?? '?'}`;
  }

  get podePlanejar(): boolean {
    return this.ehStatus(['PLANEJADA', 'PROGRAMADA']);
  }

  get podeConfirmarChegada(): boolean {
    return this.ehStatus(['PROGRAMADA']);
  }

  get podeAtracar(): boolean {
    return this.ehStatus(['PROGRAMADA', 'CHEGADA_CONFIRMADA']);
  }

  get podeIniciar(): boolean {
    return this.ehStatus(['ATRACADA']);
  }

  get podeConcluir(): boolean {
    return this.ehStatus(['EM_OPERACAO']);
  }

  get podeDesatracar(): boolean {
    return this.ehStatus(['ATRACADA', 'EM_OPERACAO', 'OPERACAO_CONCLUIDA']);
  }

  get podeCancelar(): boolean {
    return !this.ehStatus(['DESATRACADA', 'CANCELADA']);
  }

  private ehStatus(status: string[]): boolean {
    return this.visita != null && status.indexOf(this.visita.status) >= 0;
  }

  private executar(observavel: Observable<VisitaNavioDetalhe>, mensagemSucesso: string): void {
    observavel.subscribe({
      next: (dados: VisitaNavioDetalhe) => {
        this.aplicar(dados);
        this.mensagem = mensagemSucesso;
        this.erro = null;
      },
      error: (resposta: { error?: { mensagem?: string } }) => {
        this.erro = resposta?.error?.mensagem ?? 'Não foi possível concluir a operação.';
      }
    });
  }

  private aplicar(dados: VisitaNavioDetalhe): void {
    this.visita = dados;
    this.planejamento = {
      bercoId: dados.bercoId ?? 0,
      atracacaoPrevista: this.paraInput(dados.atracacaoPrevista),
      desatracacaoPrevista: this.paraInput(dados.desatracacaoPrevista)
    };
  }

  private paraInput(valor: string | null | undefined): string {
    return valor ? valor.substring(0, 16) : '';
  }

  private comSegundos(valor: string): string {
    return valor && valor.length === 16 ? `${valor}:00` : valor;
  }

  private operacaoVazia(): OperacaoRequest {
    return {
      tipoOperacao: 'DESCARGA',
      identificacaoConteiner: '',
      bay: null,
      fileira: null,
      altura: null,
      pesoToneladas: null
    };
  }
}
