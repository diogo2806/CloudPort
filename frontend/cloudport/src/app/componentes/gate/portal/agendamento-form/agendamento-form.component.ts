import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Agendamento, AgendamentoFormPayload, AgendamentoRequest, DocumentoAgendamento, GateEnumOption, UploadDocumentoStatus } from '../../../model/gate/agendamento.model';
import { JanelaAtendimento } from '../../../model/gate/janela.model';

interface DocumentoPreview {
  nome: string;
  tamanho: string;
  tipo: string;
  url?: string;
  arquivo: File;
}

@Component({
  selector: 'app-agendamento-form',
  templateUrl: './agendamento-form.component.html',
  styleUrls: ['./agendamento-form.component.css']
})
export class AgendamentoFormComponent implements OnInit, OnChanges, OnDestroy {
  @Input() agendamento: Agendamento | null = null;
  @Input() tiposOperacao$: Observable<GateEnumOption[]> | null = null;
  @Input() status$: Observable<GateEnumOption[]> | null = null;
  @Input() janelas$: Observable<JanelaAtendimento[]> | null = null;
  @Input() documentosExistentes: DocumentoAgendamento[] | null = [];
  @Input() uploadStatus: UploadDocumentoStatus[] = [];

  @Output() salvar = new EventEmitter<AgendamentoFormPayload>();
  @Output() cancelar = new EventEmitter<void>();

  formulario!: FormGroup;
  documentosSelecionados: DocumentoPreview[] = [];
  janelasDisponiveis: JanelaAtendimento[] = [];

  private readonly destruir$ = new Subject<void>();

  constructor(private readonly formBuilder: FormBuilder) {}

  ngOnInit(): void {
    this.formulario = this.formBuilder.group({
      codigo: ['', Validators.required],
      tipoOperacao: [null, Validators.required],
      status: [null, Validators.required],
      transportadoraId: [null, Validators.required],
      motoristaId: [null, Validators.required],
      motoristaCpf: ['', [Validators.required, this.cpfValidator()]],
      veiculoId: [null, Validators.required],
      placaVeiculo: ['', [Validators.required, this.placaValidator()]],
      janelaAtendimentoId: [null, Validators.required],
      horarioPrevistoChegada: ['', Validators.required],
      horarioPrevistoSaida: ['', Validators.required],
      observacoes: [''],
      documentos: [[], this.documentosValidator()]
    }, { validators: [this.horarioDentroDaJanelaValidator()] });

    if (this.janelas$) {
      this.janelas$
        .pipe(takeUntil(this.destruir$))
        .subscribe((janelas) => {
          this.janelasDisponiveis = janelas;
          this.formulario.updateValueAndValidity({ emitEvent: false });
        });
    }

    this.preencherFormulario();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['agendamento'] && !changes['agendamento'].firstChange) {
      this.preencherFormulario();
    }
    if (changes['documentosExistentes'] && this.formulario) {
      this.formulario.get('documentos')?.updateValueAndValidity();
    }
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
    this.liberarPreviews();
  }

  get emEdicao(): boolean {
    return !!this.agendamento;
  }

  onSubmit(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valor = this.formulario.value;

    const request: AgendamentoRequest = {
      codigo: valor.codigo,
      tipoOperacao: valor.tipoOperacao,
      status: valor.status,
      transportadoraId: Number(valor.transportadoraId),
      motoristaId: Number(valor.motoristaId),
      veiculoId: Number(valor.veiculoId),
      janelaAtendimentoId: Number(valor.janelaAtendimentoId),
      horarioPrevistoChegada: valor.horarioPrevistoChegada,
      horarioPrevistoSaida: valor.horarioPrevistoSaida,
      observacoes: valor.observacoes,
      placaVeiculo: this.sanitizarPlaca(valor.placaVeiculo),
      motoristaCpf: this.sanitizarCpf(valor.motoristaCpf)
    };

    const arquivos = this.documentosSelecionados.map((preview) => preview.arquivo);

    this.salvar.emit({
      request,
      arquivos
    });
  }

  cancelarEdicao(): void {
    this.cancelar.emit();
    this.liberarPreviews();
    this.formulario.reset();
    this.documentosSelecionados = [];
    this.formulario.get('documentos')?.setValue([]);
  }

  aoSelecionarArquivos(evento: Event): void {
    const input = evento.target as HTMLInputElement | null;
    const arquivos = Array.from(input?.files ?? []);
    this.liberarPreviews();
    this.documentosSelecionados = arquivos.map((arquivo) => ({
      nome: arquivo.name,
      tamanho: this.formatarTamanho(arquivo.size),
      tipo: arquivo.type || 'application/octet-stream',
      url: arquivo.type.startsWith('image/') ? URL.createObjectURL(arquivo) : undefined,
      arquivo
    }));
    this.formulario.get('documentos')?.setValue(this.documentosSelecionados.map((item) => item.arquivo));
    this.formulario.get('documentos')?.markAsDirty();
    this.formulario.get('documentos')?.updateValueAndValidity();
  }

  acompanharProgresso(nomeArquivo: string): UploadDocumentoStatus | undefined {
    return this.uploadStatus.find((status) => status.fileName === nomeArquivo);
  }

  aplicarMascaraCpf(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const valorFormatado = this.formatarCpf(input.value);
    input.value = valorFormatado;
    this.formulario.get('motoristaCpf')?.setValue(valorFormatado, { emitEvent: false });
  }

  aplicarMascaraPlaca(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const valorFormatado = this.formatarPlaca(input.value);
    input.value = valorFormatado;
    this.formulario.get('placaVeiculo')?.setValue(valorFormatado, { emitEvent: false });
  }

  private preencherFormulario(): void {
    if (!this.formulario) {
      return;
    }

    this.liberarPreviews();
    this.documentosSelecionados = [];

    if (this.agendamento) {
      this.formulario.patchValue({
        codigo: this.agendamento.codigo,
        tipoOperacao: this.agendamento.tipoOperacao,
        status: this.agendamento.status,
        transportadoraId: this.agendamento.transportadoraId,
        motoristaId: this.agendamento.motoristaId,
        motoristaCpf: '',
        veiculoId: this.agendamento.veiculoId,
        placaVeiculo: this.formatarPlaca(this.agendamento.placaVeiculo ?? ''),
        janelaAtendimentoId: this.agendamento.janelaAtendimentoId,
        horarioPrevistoChegada: this.agendamento.horarioPrevistoChegada,
        horarioPrevistoSaida: this.agendamento.horarioPrevistoSaida,
        observacoes: this.agendamento.observacoes || ''
      });
    } else {
      this.formulario.reset();
      this.formulario.get('documentos')?.setValue([]);
    }

    this.formulario.get('documentos')?.updateValueAndValidity();
  }

  private horarioDentroDaJanelaValidator(): ValidatorFn {
    return (controle: AbstractControl): ValidationErrors | null => {
      const janelaId = controle.get('janelaAtendimentoId')?.value;
      const chegada = controle.get('horarioPrevistoChegada')?.value;
      const saida = controle.get('horarioPrevistoSaida')?.value;

      if (!janelaId || !chegada || !saida) {
        return null;
      }

      const janela = this.janelasDisponiveis.find((item) => item.id === Number(janelaId));
      if (!janela) {
        return null;
      }

      const inicio = this.converterHoraParaMinutos(janela.horaInicio);
      const fim = this.converterHoraParaMinutos(janela.horaFim);
      const chegadaMin = this.converterHoraParaMinutos(chegada);
      const saidaMin = this.converterHoraParaMinutos(saida);

      if (chegadaMin < inicio || saidaMin > fim || chegadaMin >= saidaMin) {
        return { horarioJanelaInvalido: true };
      }

      return null;
    };
  }

  private documentosValidator(): ValidatorFn {
    return (controle: AbstractControl): ValidationErrors | null => {
      const arquivosSelecionados = Array.isArray(controle.value) ? controle.value : [];
      const possuiExistentes = (this.documentosExistentes?.length ?? 0) > 0;
      if (!arquivosSelecionados.length && !possuiExistentes) {
        return { documentosObrigatorios: true };
      }
      return null;
    };
  }

  private cpfValidator(): ValidatorFn {
    return (controle: AbstractControl): ValidationErrors | null => {
      const valor = (controle.value ?? '') as string;
      const cpf = this.sanitizarCpf(valor);
      if (!cpf || cpf.length !== 11 || /^([0-9])\1+$/.test(cpf)) {
        return { cpfInvalido: true };
      }

      const digitoValido = (base: number): number => {
        let soma = 0;
        for (let i = 0; i < base; i++) {
          soma += parseInt(cpf.charAt(i), 10) * (base + 1 - i);
        }
        const resto = (soma * 10) % 11;
        return resto === 10 ? 0 : resto;
      };

      if (digitoValido(9) !== parseInt(cpf.charAt(9), 10) || digitoValido(10) !== parseInt(cpf.charAt(10), 10)) {
        return { cpfInvalido: true };
      }

      return null;
    };
  }

  private placaValidator(): ValidatorFn {
    const regex = /^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$/;
    return (controle: AbstractControl): ValidationErrors | null => {
      const valor = (controle.value ?? '') as string;
      if (!valor) {
        return { placaInvalida: true };
      }
      const placa = valor.toUpperCase();
      if (!regex.test(placa)) {
        return { placaInvalida: true };
      }
      return null;
    };
  }

  private converterHoraParaMinutos(valor: string): number {
    const [horas, minutos] = valor.split(':').map(Number);
    return horas * 60 + minutos;
  }

  private formatarTamanho(bytes: number): string {
    if (!bytes) {
      return '0 B';
    }
    const unidades = ['B', 'KB', 'MB', 'GB'];
    const indice = Math.floor(Math.log(bytes) / Math.log(1024));
    const tamanho = bytes / Math.pow(1024, indice);
    return `${tamanho.toFixed(1)} ${unidades[indice]}`;
  }

  private formatarCpf(valor: string): string {
    const numeros = valor.replace(/\D/g, '').slice(0, 11);
    const partes = [
      numeros.slice(0, 3),
      numeros.slice(3, 6),
      numeros.slice(6, 9),
      numeros.slice(9, 11)
    ].filter(Boolean);
    if (partes.length > 3) {
      return `${partes[0]}.${partes[1]}.${partes[2]}-${partes[3]}`;
    }
    if (partes.length > 2) {
      return `${partes[0]}.${partes[1]}.${partes[2]}`;
    }
    if (partes.length > 1) {
      return `${partes[0]}.${partes[1]}`;
    }
    return partes[0] ?? '';
  }

  private formatarPlaca(valor: string): string {
    const caracteres = valor.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
    if (caracteres.length <= 3) {
      return caracteres;
    }
    return `${caracteres.slice(0, 3)}-${caracteres.slice(3)}`;
  }

  private sanitizarCpf(valor: string): string | null {
    const digits = valor.replace(/\D/g, '');
    return digits.length ? digits : null;
  }

  private sanitizarPlaca(valor: string): string | null {
    const sanitized = valor.toUpperCase().replace(/[^A-Z0-9]/g, '');
    return sanitized.length ? sanitized : null;
  }

  private liberarPreviews(): void {
    this.documentosSelecionados
      .filter((preview) => preview.url)
      .forEach((preview) => URL.revokeObjectURL(preview.url!));
  }
}

