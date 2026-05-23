import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Controla o fator de escala do texto da aplicação (acessibilidade).
 * O valor é expresso em porcentagem e aplicado ao tamanho de fonte da raiz,
 * fazendo com que as unidades relativas (rem/em) acompanhem a preferência.
 */
@Injectable({ providedIn: 'root' })
export class TamanhoTextoService {
  static readonly TAMANHO_PADRAO = 100;
  static readonly TAMANHO_MINIMO = 80;
  static readonly TAMANHO_MAXIMO = 150;
  static readonly PASSO = 10;

  private static readonly CHAVE_ARMAZENAMENTO = 'cloudport.tamanho-texto';

  private readonly tamanhoSubject = new BehaviorSubject<number>(this.carregarTamanhoInicial());
  readonly tamanho$: Observable<number> = this.tamanhoSubject.asObservable();

  constructor() {
    this.aplicar(this.tamanhoSubject.value);
  }

  get tamanhoAtual(): number {
    return this.tamanhoSubject.value;
  }

  get tamanhoMinimo(): number {
    return TamanhoTextoService.TAMANHO_MINIMO;
  }

  get tamanhoMaximo(): number {
    return TamanhoTextoService.TAMANHO_MAXIMO;
  }

  aumentar(): void {
    this.definir(this.tamanhoSubject.value + TamanhoTextoService.PASSO);
  }

  diminuir(): void {
    this.definir(this.tamanhoSubject.value - TamanhoTextoService.PASSO);
  }

  redefinir(): void {
    this.definir(TamanhoTextoService.TAMANHO_PADRAO);
  }

  definir(valor: number): void {
    const tamanho = this.normalizar(valor);
    if (tamanho === this.tamanhoSubject.value) {
      this.aplicar(tamanho);
      return;
    }
    this.tamanhoSubject.next(tamanho);
    this.persistir(tamanho);
    this.aplicar(tamanho);
  }

  private normalizar(valor: number): number {
    if (!Number.isFinite(valor)) {
      return TamanhoTextoService.TAMANHO_PADRAO;
    }
    const arredondado = Math.round(valor);
    return Math.min(
      TamanhoTextoService.TAMANHO_MAXIMO,
      Math.max(TamanhoTextoService.TAMANHO_MINIMO, arredondado)
    );
  }

  private aplicar(valor: number): void {
    if (typeof document === 'undefined') {
      return;
    }
    const raiz = document.documentElement;
    raiz.style.setProperty('--app-font-scale', String(valor / 100));
    raiz.style.fontSize = `${valor}%`;
  }

  private carregarTamanhoInicial(): number {
    if (typeof localStorage === 'undefined') {
      return TamanhoTextoService.TAMANHO_PADRAO;
    }
    try {
      const armazenado = localStorage.getItem(TamanhoTextoService.CHAVE_ARMAZENAMENTO);
      if (armazenado === null) {
        return TamanhoTextoService.TAMANHO_PADRAO;
      }
      return this.normalizar(Number(armazenado));
    } catch {
      return TamanhoTextoService.TAMANHO_PADRAO;
    }
  }

  private persistir(valor: number): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      localStorage.setItem(TamanhoTextoService.CHAVE_ARMAZENAMENTO, String(valor));
    } catch {
      // Ignora ambientes sem armazenamento persistente (modo privado, cotas, etc.).
    }
  }
}
