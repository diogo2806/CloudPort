import { Component, OnInit } from '@angular/core';
import {
  Berco,
  BercoRequest,
  ServicoNavioService,
  StatusBerco
} from '../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-navio-lista-bercos',
  templateUrl: './lista-bercos.component.html',
  standalone: false
})
export class ListaBercosComponent implements OnInit {
  bercos: Berco[] = [];
  carregando = false;
  erro: string | null = null;
  statusDisponiveis: StatusBerco[] = ['DISPONIVEL', 'OCUPADO', 'INATIVO'];

  edicaoId: number | null = null;
  formulario: BercoRequest = this.formularioVazio();

  constructor(private readonly servico: ServicoNavioService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.servico.listarBercos().subscribe({
      next: (dados) => {
        this.bercos = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar os berços.';
        this.carregando = false;
      }
    });
  }

  editar(berco: Berco): void {
    this.edicaoId = berco.identificador;
    this.formulario = {
      nome: berco.nome,
      comprimentoMetros: berco.comprimentoMetros,
      caladoMaximoMetros: berco.caladoMaximoMetros,
      status: berco.status
    };
  }

  cancelarEdicao(): void {
    this.edicaoId = null;
    this.formulario = this.formularioVazio();
  }

  salvar(): void {
    if (!this.formulario.nome || !this.formulario.comprimentoMetros || !this.formulario.caladoMaximoMetros) {
      this.erro = 'Preencha nome, comprimento e calado máximo.';
      return;
    }
    const requisicao = this.edicaoId == null
      ? this.servico.criarBerco(this.formulario)
      : this.servico.atualizarBerco(this.edicaoId, this.formulario);
    requisicao.subscribe({
      next: () => {
        this.cancelarEdicao();
        this.carregar();
      },
      error: () => (this.erro = 'Não foi possível salvar o berço.')
    });
  }

  remover(berco: Berco): void {
    this.servico.removerBerco(berco.identificador).subscribe({
      next: () => this.carregar(),
      error: () => (this.erro = 'Não foi possível remover o berço (verifique se há visitas vinculadas).')
    });
  }

  private formularioVazio(): BercoRequest {
    return { nome: '', comprimentoMetros: 0, caladoMaximoMetros: 0, status: 'DISPONIVEL' };
  }
}
