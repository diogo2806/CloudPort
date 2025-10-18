import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { ConteinerMapa, EquipamentoMapa, OpcoesCadastroPatio, ServicoPatioService } from '../../service/servico-patio/servico-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

@Component({
  selector: 'app-formulario-movimentacao',
  templateUrl: './formulario-movimentacao.component.html',
  styleUrls: ['./formulario-movimentacao.component.css']
})
export class FormularioMovimentacaoComponent implements OnInit {
  formularioConteiner: FormGroup;
  formularioEquipamento: FormGroup;
  opcoes?: OpcoesCadastroPatio;
  carregandoOpcoes = false;
  salvandoConteiner = false;
  salvandoEquipamento = false;
  sucessoConteiner?: string;
  erroConteiner?: string;
  sucessoEquipamento?: string;
  erroEquipamento?: string;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly servicoPatio: ServicoPatioService,
    private readonly sanitizador: SanitizadorConteudoService
  ) {
    this.formularioConteiner = this.formBuilder.group({
      id: [null],
      codigo: ['', [Validators.required, Validators.maxLength(30)]],
      linha: [0, [Validators.required, Validators.min(0)]],
      coluna: [0, [Validators.required, Validators.min(0)]],
      status: ['', Validators.required],
      tipoCarga: ['', [Validators.required, Validators.maxLength(40)]],
      destino: ['', [Validators.required, Validators.maxLength(60)]],
      camadaOperacional: ['', [Validators.required, Validators.maxLength(40)]]
    });

    this.formularioEquipamento = this.formBuilder.group({
      id: [null],
      identificador: ['', [Validators.required, Validators.maxLength(30)]],
      tipoEquipamento: ['', Validators.required],
      linha: [0, [Validators.required, Validators.min(0)]],
      coluna: [0, [Validators.required, Validators.min(0)]],
      statusOperacional: ['', Validators.required]
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

  submeterConteiner(): void {
    if (this.formularioConteiner.invalid) {
      this.formularioConteiner.markAllAsTouched();
      return;
    }
    const valores = this.formularioConteiner.getRawValue();
    const payload: ConteinerMapa = {
      id: valores.id ?? undefined,
      codigo: this.sanitizarTexto(valores.codigo),
      linha: Number(valores.linha),
      coluna: Number(valores.coluna),
      status: valores.status,
      tipoCarga: this.sanitizarTexto(valores.tipoCarga),
      destino: this.sanitizarTexto(valores.destino),
      camadaOperacional: this.sanitizarTexto(valores.camadaOperacional)
    };

    this.salvandoConteiner = true;
    this.sucessoConteiner = undefined;
    this.erroConteiner = undefined;
    this.servicoPatio.salvarConteiner(payload)
      .pipe(finalize(() => this.salvandoConteiner = false))
      .subscribe({
        next: (resposta) => {
          this.sucessoConteiner = 'Contêiner atualizado com sucesso.';
          this.formularioConteiner.patchValue({ id: resposta.id ?? null });
        },
        error: () => {
          this.erroConteiner = 'Não foi possível registrar a movimentação do contêiner. Verifique os dados informados.';
        }
      });
  }

  submeterEquipamento(): void {
    if (this.formularioEquipamento.invalid) {
      this.formularioEquipamento.markAllAsTouched();
      return;
    }
    const valores = this.formularioEquipamento.getRawValue();
    const payload: EquipamentoMapa = {
      id: valores.id ?? undefined,
      identificador: this.sanitizarTexto(valores.identificador),
      tipoEquipamento: valores.tipoEquipamento,
      linha: Number(valores.linha),
      coluna: Number(valores.coluna),
      statusOperacional: valores.statusOperacional
    };

    this.salvandoEquipamento = true;
    this.sucessoEquipamento = undefined;
    this.erroEquipamento = undefined;
    this.servicoPatio.salvarEquipamento(payload)
      .pipe(finalize(() => this.salvandoEquipamento = false))
      .subscribe({
        next: (resposta) => {
          this.sucessoEquipamento = 'Equipamento atualizado com sucesso.';
          this.formularioEquipamento.patchValue({ id: resposta.id ?? null });
        },
        error: () => {
          this.erroEquipamento = 'Não foi possível atualizar as informações do equipamento.';
        }
      });
  }

  limparFormularioConteiner(): void {
    this.formularioConteiner.reset({ linha: 0, coluna: 0 });
    this.sucessoConteiner = undefined;
    this.erroConteiner = undefined;
  }

  limparFormularioEquipamento(): void {
    this.formularioEquipamento.reset({ linha: 0, coluna: 0 });
    this.sucessoEquipamento = undefined;
    this.erroEquipamento = undefined;
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
