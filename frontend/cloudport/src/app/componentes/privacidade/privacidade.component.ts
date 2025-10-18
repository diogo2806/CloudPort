import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, of } from 'rxjs';
import { catchError, finalize, takeUntil } from 'rxjs/operators';
import { OpcaoPrivacidade, PrivacidadeService } from '../service/privacidade/privacidade.service';

@Component({
  selector: 'app-privacidade',
  templateUrl: './privacidade.component.html',
  styleUrls: ['./privacidade.component.css']
})
export class PrivacidadeComponent implements OnInit, OnDestroy {
  opcoes: OpcaoPrivacidade[] = [];
  carregando = false;
  mensagemErro: string | null = null;

  private readonly destruir$ = new Subject<void>();

  constructor(private readonly privacidadeService: PrivacidadeService) {}

  ngOnInit(): void {
    this.carregarOpcoes();
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  recarregar(): void {
    this.carregarOpcoes();
  }

  private carregarOpcoes(): void {
    this.carregando = true;
    this.mensagemErro = null;
    this.privacidadeService
      .listarOpcoes()
      .pipe(
        takeUntil(this.destruir$),
        catchError(() => {
          this.mensagemErro = 'Não foi possível carregar as opções de privacidade. Tente novamente.';
          return of([]);
        }),
        finalize(() => {
          this.carregando = false;
        })
      )
      .subscribe((opcoes) => {
        this.opcoes = opcoes;
      });
  }
}
