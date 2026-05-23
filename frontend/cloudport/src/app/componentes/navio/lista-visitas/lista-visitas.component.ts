import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  CadastroVisitaNavio,
  NavioResumo,
  ServicoNavioService,
  VisitaNavioResumo
} from '../../service/servico-navio/servico-navio.service';

@Component({
  selector: 'app-navio-lista-visitas',
  templateUrl: './lista-visitas.component.html',
  standalone: false
})
export class ListaVisitasComponent implements OnInit {
  visitas: VisitaNavioResumo[] = [];
  navios: NavioResumo[] = [];
  carregando = false;
  erro: string | null = null;
  mensagem: string | null = null;
  exibirFormulario = false;
  nova: CadastroVisitaNavio = this.formularioVazio();

  constructor(
    private readonly servico: ServicoNavioService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.carregar();
    this.servico.listarNavios().subscribe({
      next: (dados) => (this.navios = dados),
      error: () => undefined
    });
  }

  carregar(): void {
    this.carregando = true;
    this.erro = null;
    this.servico.listarVisitas().subscribe({
      next: (dados) => {
        this.visitas = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar as visitas de navio.';
        this.carregando = false;
      }
    });
  }

  alternarFormulario(): void {
    this.exibirFormulario = !this.exibirFormulario;
    this.mensagem = null;
    this.erro = null;
  }

  registrar(): void {
    if (!this.nova.navioId || !this.nova.numeroViagem || !this.nova.atracacaoPrevista || !this.nova.desatracacaoPrevista) {
      this.erro = 'Preencha o navio, o número da viagem e a janela de atracação.';
      return;
    }
    const payload: CadastroVisitaNavio = {
      navioId: this.nova.navioId,
      numeroViagem: this.nova.numeroViagem,
      observacoes: this.nova.observacoes,
      atracacaoPrevista: this.comSegundos(this.nova.atracacaoPrevista),
      desatracacaoPrevista: this.comSegundos(this.nova.desatracacaoPrevista)
    };
    this.servico.criarVisita(payload).subscribe({
      next: (visita) => {
        this.nova = this.formularioVazio();
        this.exibirFormulario = false;
        this.router.navigate(['/home/navio/visitas', visita.identificador]);
      },
      error: () => (this.erro = 'Não foi possível registrar a visita.')
    });
  }

  abrir(visita: VisitaNavioResumo): void {
    this.router.navigate(['/home/navio/visitas', visita.identificador]);
  }

  private comSegundos(valor: string): string {
    return valor && valor.length === 16 ? `${valor}:00` : valor;
  }

  private formularioVazio(): CadastroVisitaNavio {
    return { navioId: 0, numeroViagem: '', atracacaoPrevista: '', desatracacaoPrevista: '', observacoes: '' };
  }
}
