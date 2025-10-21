import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { OpcoesCadastroPatio, ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { NovaOrdemTrabalhoPatio, ServicoListaTrabalhoPatioService, TipoMovimentoPatio } from '../../service/servico-lista-trabalho-patio/servico-lista-trabalho-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

@Component({
  selector: 'app-formulario-movimentacao',
  templateUrl: './formulario-movimentacao.component.html',
  styleUrls: ['./formulario-movimentacao.component.css']
})
export class FormularioMovimentacaoComponent implements OnInit {
  formularioOrdem: FormGroup;
  opcoes?: OpcoesCadastroPatio;
  carregandoOpcoes = false;
  salvandoOrdem = false;
  sucessoOrdem?: string;
  erroOrdem?: string;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly servicoPatio: ServicoPatioService,
    private readonly servicoListaTrabalho: ServicoListaTrabalhoPatioService,
    private readonly sanitizador: SanitizadorConteudoService
  ) {
    this.formularioOrdem = this.formBuilder.group({
      codigo: ['', [Validators.required, Validators.maxLength(30)]],
      tipoCarga: ['', [Validators.required, Validators.maxLength(40)]],
      destino: ['', [Validators.required, Validators.maxLength(60)]],
      linhaDestino: [0, [Validators.required, Validators.min(0)]],
      colunaDestino: [0, [Validators.required, Validators.min(0)]],
      camadaDestino: ['', [Validators.required, Validators.maxLength(40)]],
      tipoMovimento: ['', Validators.required],
      statusConteinerDestino: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.carregarOpcoes();
  }

  carregarOpcoes(): void {
    this.carregandoOpcoes = true;
    this.servicoPatio.obterOpcoesCadastro()
      .pipe(finalize(() => this.carregandoOpcoes = false))
      .subscribe({
        next: (opcoes) => {
          this.opcoes = opcoes;
        },
        error: () => {
          this.opcoes = undefined;
        }
      });
  }

  submeterOrdem(): void {
    if (this.formularioOrdem.invalid) {
      this.formularioOrdem.markAllAsTouched();
      return;
    }
    const valores = this.formularioOrdem.getRawValue();
    const payload: NovaOrdemTrabalhoPatio = {
      codigoConteiner: this.sanitizarTexto(valores.codigo),
      tipoCarga: this.sanitizarTexto(valores.tipoCarga),
      destino: this.sanitizarTexto(valores.destino),
      linhaDestino: Number(valores.linhaDestino),
      colunaDestino: Number(valores.colunaDestino),
      camadaDestino: this.sanitizarTexto(valores.camadaDestino),
      tipoMovimento: valores.tipoMovimento as TipoMovimentoPatio,
      statusConteinerDestino: valores.statusConteinerDestino
    };

    this.salvandoOrdem = true;
    this.sucessoOrdem = undefined;
    this.erroOrdem = undefined;
    this.servicoListaTrabalho.registrarOrdem(payload)
      .pipe(finalize(() => (this.salvandoOrdem = false)))
      .subscribe({
        next: () => {
          this.sucessoOrdem = 'Ordem de trabalho criada com sucesso. Ela será exibida imediatamente para os operadores.';
          this.formularioOrdem.reset({ linhaDestino: 0, colunaDestino: 0 });
        },
        error: () => {
          this.erroOrdem = 'Não foi possível registrar a ordem de trabalho. Verifique os dados informados.';
        }
      });
  }

  limparFormularioOrdem(): void {
    this.formularioOrdem.reset({ linhaDestino: 0, colunaDestino: 0 });
    this.sucessoOrdem = undefined;
    this.erroOrdem = undefined;
  }

  formatarRotulo(valor: string): string {
    const texto = this.sanitizarTexto(valor);
    return texto
      .toLowerCase()
      .split(/[_\s]+/)
      .filter(parte => parte.length > 0)
      .map(parte => parte.charAt(0).toUpperCase() + parte.slice(1))
      .join(' ');
  }

  private sanitizarTexto(valor: string): string {
    return this.sanitizador.sanitizar(valor ?? '').trim();
  }
}
