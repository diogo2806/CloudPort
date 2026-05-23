import { Component, OnInit } from '@angular/core';
import {
  PortoRotacao,
  ServicoLinha,
  ServicoLinhaRequest,
  ServicoNavioService
} from '../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-navio-servicos-linha',
  templateUrl: './servicos-linha.component.html',
  standalone: false
})
export class ServicosLinhaComponent implements OnInit {
  servicos: ServicoLinha[] = [];
  carregando = false;
  erro: string | null = null;

  edicaoId: number | null = null;
  formulario: ServicoLinhaRequest = this.formularioVazio();
  novoPorto: PortoRotacao = this.portoVazio();

  constructor(private readonly servico: ServicoNavioService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.servico.listarServicos().subscribe({
      next: (dados) => {
        this.servicos = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar os serviços de linha.';
        this.carregando = false;
      }
    });
  }

  editar(servico: ServicoLinha): void {
    this.edicaoId = servico.identificador;
    this.formulario = {
      codigo: servico.codigo,
      nome: servico.nome,
      armador: servico.armador ?? '',
      rotacao: servico.rotacao.map((porto) => ({
        sequencia: porto.sequencia,
        portoUnloc: porto.portoUnloc,
        nomePorto: porto.nomePorto
      }))
    };
  }

  cancelarEdicao(): void {
    this.edicaoId = null;
    this.formulario = this.formularioVazio();
    this.novoPorto = this.portoVazio();
  }

  adicionarPorto(): void {
    if (!this.novoPorto.portoUnloc || this.novoPorto.sequencia == null) {
      this.erro = 'Informe a sequência e o código UN/LOCODE do porto.';
      return;
    }
    this.formulario.rotacao = [...this.formulario.rotacao, { ...this.novoPorto }];
    this.novoPorto = this.portoVazio();
  }

  removerPorto(indice: number): void {
    this.formulario.rotacao = this.formulario.rotacao.filter((_, i) => i !== indice);
  }

  salvar(): void {
    if (!this.formulario.codigo || !this.formulario.nome) {
      this.erro = 'Informe o código e o nome do serviço.';
      return;
    }
    const requisicao = this.edicaoId == null
      ? this.servico.criarServico(this.formulario)
      : this.servico.atualizarServico(this.edicaoId, this.formulario);
    requisicao.subscribe({
      next: () => {
        this.cancelarEdicao();
        this.carregar();
      },
      error: () => (this.erro = 'Não foi possível salvar o serviço de linha.')
    });
  }

  remover(servico: ServicoLinha): void {
    this.servico.removerServico(servico.identificador).subscribe({
      next: () => this.carregar(),
      error: () => (this.erro = 'Não foi possível remover o serviço (verifique se há visitas vinculadas).')
    });
  }

  rotacaoResumo(servico: ServicoLinha): string {
    return servico.rotacao.map((porto) => porto.portoUnloc).join(' → ') || '—';
  }

  private formularioVazio(): ServicoLinhaRequest {
    return { codigo: '', nome: '', armador: '', rotacao: [] };
  }

  private portoVazio(): PortoRotacao {
    return { sequencia: this.formulario ? this.formulario.rotacao.length + 1 : 1, portoUnloc: '', nomePorto: '' };
  }
}
