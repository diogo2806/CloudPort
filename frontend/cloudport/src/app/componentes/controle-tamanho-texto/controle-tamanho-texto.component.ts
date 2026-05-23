import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { TamanhoTextoService } from '../service/tamanho-texto/tamanho-texto.service';

@Component({
    selector: 'app-controle-tamanho-texto',
    templateUrl: './controle-tamanho-texto.component.html',
    styleUrls: ['./controle-tamanho-texto.component.css'],
    standalone: false
})
export class ControleTamanhoTextoComponent implements OnInit, OnDestroy {
  tamanho = TamanhoTextoService.TAMANHO_PADRAO;
  private tamanhoSubscription?: Subscription;

  constructor(private readonly tamanhoTextoService: TamanhoTextoService) {}

  ngOnInit(): void {
    this.tamanhoSubscription = this.tamanhoTextoService.tamanho$.subscribe(
      (valor) => (this.tamanho = valor)
    );
  }

  ngOnDestroy(): void {
    this.tamanhoSubscription?.unsubscribe();
  }

  get podeAumentar(): boolean {
    return this.tamanho < this.tamanhoTextoService.tamanhoMaximo;
  }

  get podeDiminuir(): boolean {
    return this.tamanho > this.tamanhoTextoService.tamanhoMinimo;
  }

  aumentar(): void {
    this.tamanhoTextoService.aumentar();
  }

  diminuir(): void {
    this.tamanhoTextoService.diminuir();
  }

  redefinir(): void {
    this.tamanhoTextoService.redefinir();
  }
}
