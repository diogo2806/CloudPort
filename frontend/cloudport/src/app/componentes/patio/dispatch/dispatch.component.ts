import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CadastroInstrucao,
  EquipamentoResumo,
  InstrucaoMovimentacao,
  ServicoDispatchService,
  TipoMoveVmt
} from '../../service/servico-dispatch/servico-dispatch.service';

@Component({
  selector: 'app-patio-dispatch',
  templateUrl: './dispatch.component.html',
  standalone: false
})
export class DispatchComponent implements OnInit {
  instrucoes: InstrucaoMovimentacao[] = [];
  equipamentos: EquipamentoResumo[] = [];
  jobList: InstrucaoMovimentacao[] = [];
  equipamentoSelecionadoId = 0;
  carregando = false;
  erro: string | null = null;
  mensagem: string | null = null;
  exibirFormulario = false;
  nova: CadastroInstrucao = this.formularioVazio();

  readonly tiposMove: TipoMoveVmt[] = [
    'RECEBIMENTO',
    'ENTREGA',
    'DESCARGA_NAVIO',
    'EMBARQUE_NAVIO',
    'DESCARGA_FERROVIA',
    'EMBARQUE_FERROVIA',
    'MOVIMENTACAO_PATIO'
  ];

  constructor(private readonly servico: ServicoDispatchService) {}

  ngOnInit(): void {
    this.servico.listarEquipamentos().subscribe({
      next: (dados) => (this.equipamentos = dados),
      error: () => undefined
    });
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.servico.listarInstrucoes().subscribe({
      next: (dados) => {
        this.instrucoes = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar as instruções de movimentação.';
        this.carregando = false;
      }
    });
  }

  carregarJobList(): void {
    if (!this.equipamentoSelecionadoId) {
      this.jobList = [];
      return;
    }
    this.servico.jobList(this.equipamentoSelecionadoId).subscribe({
      next: (dados) => (this.jobList = dados),
      error: () => (this.erro = 'Não foi possível carregar a job list do equipamento.')
    });
  }

  alternarFormulario(): void {
    this.exibirFormulario = !this.exibirFormulario;
    this.mensagem = null;
    this.erro = null;
  }

  planejar(): void {
    if (!this.nova.codigoConteiner || !this.nova.tipoMove) {
      this.erro = 'Informe ao menos o contêiner e o tipo de movimentação.';
      return;
    }
    this.servico.planejar(this.nova).subscribe({
      next: () => {
        this.nova = this.formularioVazio();
        this.exibirFormulario = false;
        this.mensagem = 'Instrução planejada.';
        this.atualizarTudo();
      },
      error: () => (this.erro = 'Não foi possível planejar a instrução.')
    });
  }

  despachar(instrucao: InstrucaoMovimentacao): void {
    if (!this.equipamentoSelecionadoId) {
      this.erro = 'Selecione um equipamento para o dispatch.';
      return;
    }
    this.executar(this.servico.despachar(instrucao.id, this.equipamentoSelecionadoId), 'Instrução despachada.');
  }

  iniciar(instrucao: InstrucaoMovimentacao): void {
    this.executar(this.servico.iniciar(instrucao.id), 'Movimentação iniciada.');
  }

  concluir(instrucao: InstrucaoMovimentacao): void {
    this.executar(this.servico.concluir(instrucao.id), 'Movimentação concluída.');
  }

  cancelar(instrucao: InstrucaoMovimentacao): void {
    this.executar(this.servico.cancelar(instrucao.id), 'Instrução cancelada.');
  }

  private executar(observavel: Observable<InstrucaoMovimentacao>, mensagemSucesso: string): void {
    observavel.subscribe({
      next: () => {
        this.mensagem = mensagemSucesso;
        this.erro = null;
        this.atualizarTudo();
      },
      error: (resposta: { error?: { mensagem?: string } }) => {
        this.erro = resposta?.error?.mensagem ?? 'Não foi possível concluir a operação.';
      }
    });
  }

  private atualizarTudo(): void {
    this.carregar();
    this.carregarJobList();
  }

  private formularioVazio(): CadastroInstrucao {
    return {
      codigoConteiner: '',
      tipoMove: 'DESCARGA_NAVIO',
      posicaoOrigem: '',
      posicaoDestino: '',
      isoTipo: '',
      lineOperator: '',
      filaTrabalho: '',
      comprimentoPes: null,
      pesoKg: null,
      sequencia: null,
      prioridadeFetch: false,
      moveTwin: false,
      requerEnergia: false,
      perigoso: false,
      foraDeBitola: false
    };
  }
}
