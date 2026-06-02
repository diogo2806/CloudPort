import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  BercoResumo,
  CalendarioBerco,
  EquipamentoBerco,
  EventoRecursosTempoReal,
  ReservaBerco,
  RespostaAlocacao,
  ResumoRecursos,
  ServicoRecursosService,
  ZonaArmazenagem
} from '../../service/servico-recursos/servico-recursos.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

interface FormularioAlocacao {
  navioCodigo: FormControl<string>;
  navioNome: FormControl<string>;
  chegadaPrevista: FormControl<string>;
  saidaPrevista: FormControl<string>;
  comprimentoNavio: FormControl<number>;
  caladoNavio: FormControl<number>;
  guinchesRequeridos: FormControl<number>;
  tipoCarga: FormControl<string>;
  zonaArmazenagem: FormControl<string>;
  toneladasPrevistas: FormControl<number>;
  bercoPreferido: FormControl<string>;
  confirmar: FormControl<boolean>;
}

interface FormularioManutencao {
  bercoCodigo: FormControl<string>;
  inicio: FormControl<string>;
  fim: FormControl<string>;
  observacao: FormControl<string>;
}

@Component({
  selector: 'app-recursos-patio',
  templateUrl: './recursos-patio.component.html',
  styleUrls: ['./recursos-patio.component.css'],
  standalone: false
})
export class RecursosPatioComponent implements OnInit, OnDestroy {
  resumo?: ResumoRecursos;
  bercos: BercoResumo[] = [];
  calendario: CalendarioBerco[] = [];
  reservas: ReservaBerco[] = [];
  equipamentos: EquipamentoBerco[] = [];
  zonas: ZonaArmazenagem[] = [];
  respostaAlocacao?: RespostaAlocacao;
  carregando = false;
  carregandoCalendario = false;
  mensagemErro?: string;
  mensagemSucesso?: string;
  inscricaoTempoReal?: Subscription;

  formularioAlocacao: FormGroup<FormularioAlocacao>;
  formularioManutencao: FormGroup<FormularioManutencao>;

  constructor(
    private readonly servicoRecursos: ServicoRecursosService,
    private readonly formBuilder: FormBuilder,
    private readonly sanitizador: SanitizadorConteudoService
  ) {
    const agora = new Date();
    const inicioPadrao = this.ajustarData(agora, 2);
    const fimPadrao = this.ajustarData(agora, 4);
    this.formularioAlocacao = this.formBuilder.group({
      navioCodigo: this.formBuilder.control('', { validators: [Validators.required], nonNullable: true }),
      navioNome: this.formBuilder.control('', { validators: [Validators.required], nonNullable: true }),
      chegadaPrevista: this.formBuilder.control(this.paraIsoLocal(inicioPadrao), { validators: [Validators.required], nonNullable: true }),
      saidaPrevista: this.formBuilder.control(this.paraIsoLocal(fimPadrao), { validators: [Validators.required], nonNullable: true }),
      comprimentoNavio: this.formBuilder.control(320, { validators: [Validators.required, Validators.min(1)], nonNullable: true }),
      caladoNavio: this.formBuilder.control(13.5, { validators: [Validators.required, Validators.min(1)], nonNullable: true }),
      guinchesRequeridos: this.formBuilder.control(2, { validators: [Validators.required, Validators.min(1)], nonNullable: true }),
      tipoCarga: this.formBuilder.control('CONTAINER', { validators: [Validators.required], nonNullable: true }),
      zonaArmazenagem: this.formBuilder.control('ZONA_A', { validators: [Validators.required], nonNullable: true }),
      toneladasPrevistas: this.formBuilder.control(8000, { validators: [Validators.required, Validators.min(0)], nonNullable: true }),
      bercoPreferido: this.formBuilder.control('', { nonNullable: true }),
      confirmar: this.formBuilder.control(false, { nonNullable: true })
    });

    this.formularioManutencao = this.formBuilder.group({
      bercoCodigo: this.formBuilder.control('BERCO_004', { validators: [Validators.required], nonNullable: true }),
      inicio: this.formBuilder.control(this.paraDataLocal(inicioPadrao), { validators: [Validators.required], nonNullable: true }),
      fim: this.formBuilder.control(this.paraDataLocal(fimPadrao), { validators: [Validators.required], nonNullable: true }),
      observacao: this.formBuilder.control('Manutenção preventiva programada', { nonNullable: true })
    });
  }

  ngOnInit(): void {
    this.carregarTudo();
    this.inscricaoTempoReal = this.servicoRecursos.iniciarMonitoramentoTempoReal().subscribe({
      next: (evento) => this.processarEvento(evento),
      error: () => {
        this.mensagemErro = 'Falha na atualização em tempo real dos recursos.';
      }
    });
  }

  ngOnDestroy(): void {
    this.inscricaoTempoReal?.unsubscribe();
  }

  carregarTudo(): void {
    this.carregando = true;
    this.mensagemErro = undefined;

    this.servicoRecursos.consultarResumo().subscribe({
      next: (resumo) => {
        this.resumo = resumo;
        this.zonas = resumo.zonas ?? [];
        this.equipamentos = resumo.equipamentos ?? [];
        this.carregarListasDependentes();
      },
      error: () => {
        this.mensagemErro = 'Não foi possível carregar o resumo dos recursos.';
        this.carregando = false;
      }
    });
  }

  submeterAlocacao(): void {
    if (this.formularioAlocacao.invalid) {
      this.mensagemErro = 'Preencha os campos obrigatórios da alocação.';
      return;
    }

    const valor = this.formularioAlocacao.getRawValue();
    this.carregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;

    this.servicoRecursos.recomendarOuConfirmarAlocacao({
      navioCodigo: valor.navioCodigo,
      navioNome: valor.navioNome,
      chegadaPrevista: this.normalizarDataHora(valor.chegadaPrevista),
      saidaPrevista: this.normalizarDataHora(valor.saidaPrevista),
      comprimentoNavio: Number(valor.comprimentoNavio),
      caladoNavio: Number(valor.caladoNavio),
      guinchesRequeridos: Number(valor.guinchesRequeridos),
      tipoCarga: valor.tipoCarga,
      zonaArmazenagem: valor.zonaArmazenagem,
      toneladasPrevistas: Number(valor.toneladasPrevistas),
      bercoPreferido: valor.bercoPreferido || undefined,
      confirmar: valor.confirmar
    }).subscribe({
      next: (resposta) => {
        this.respostaAlocacao = resposta;
        this.mensagemSucesso = valor.confirmar
          ? `Alocação confirmada no ${resposta.bercoRecomendado.codigo}.`
          : `Recomendação gerada para ${resposta.bercoRecomendado.codigo}.`;
        this.carregarTudo();
      },
      error: (erro) => {
        this.mensagemErro = erro?.error || 'Não foi possível alocar o navio.';
        this.carregando = false;
      }
    });
  }

  submeterManutencao(): void {
    if (this.formularioManutencao.invalid) {
      this.mensagemErro = 'Preencha os campos da manutenção.';
      return;
    }

    const valor = this.formularioManutencao.getRawValue();
    this.carregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;

    this.servicoRecursos.agendarManutencao({
      bercoCodigo: valor.bercoCodigo,
      inicio: this.paraIsoData(valor.inicio),
      fim: this.paraIsoData(valor.fim),
      observacao: valor.observacao
    }).subscribe({
      next: () => {
        this.mensagemSucesso = `Manutenção agendada para ${valor.bercoCodigo}.`;
        this.carregarTudo();
      },
      error: (erro) => {
        this.mensagemErro = erro?.error || 'Não foi possível agendar a manutenção.';
        this.carregando = false;
      }
    });
  }

  destacarBerco(codigo: string): boolean {
    return (this.respostaAlocacao?.bercoRecomendado?.codigo ?? '').toUpperCase() === codigo.toUpperCase();
  }

  statusZona(zona: ZonaArmazenagem): string {
    if (zona.bloqueada) {
      return 'Bloqueada';
    }
    if (zona.percentualOcupacao >= 95) {
      return 'Crítica';
    }
    if (zona.percentualOcupacao >= 80) {
      return 'Atenção';
    }
    return 'Normal';
  }

  classeZona(zona: ZonaArmazenagem): string {
    if (zona.bloqueada) {
      return 'zona-bloqueada';
    }
    if (zona.percentualOcupacao >= 95) {
      return 'zona-critica';
    }
    if (zona.percentualOcupacao >= 80) {
      return 'zona-atencao';
    }
    return 'zona-normal';
  }

  diasCalendario(berco: CalendarioBerco): string[] {
    return (berco.dias ?? []).map((dia) => dia.rotulo);
  }

  sanitizar(texto: string | null | undefined): string {
    return this.sanitizador.sanitizar(texto ?? '');
  }

  formatarData(valor: string | null | undefined): string {
    if (!valor) {
      return '';
    }
    const partes = valor.split('-');
    if (partes.length !== 3) {
      return valor;
    }
    return `${partes[2]}/${partes[1]}`;
  }

  trackByCodigo(_: number, item: BercoResumo): string {
    return item.codigo;
  }

  trackByZona(_: number, item: ZonaArmazenagem): string {
    return item.codigo;
  }

  trackByCalendario(_: number, item: CalendarioBerco): string {
    return item.codigoBerco;
  }

  trackByReserva(_: number, item: ReservaBerco): number {
    return item.id;
  }

  private carregarListasDependentes(): void {
    this.servicoRecursos.listarBercos().subscribe({
      next: (bercos) => {
        this.bercos = bercos;
        this.carregarCalendario();
      },
      error: () => {
        this.mensagemErro = 'Não foi possível carregar os berços cadastrados.';
        this.carregando = false;
      }
    });

    this.servicoRecursos.listarReservas().subscribe({
      next: (reservas) => {
        this.reservas = reservas;
      },
      error: () => {
        this.mensagemErro = 'Não foi possível carregar as reservas dos berços.';
        this.carregando = false;
      }
    });
  }

  private carregarCalendario(): void {
    this.carregandoCalendario = true;
    this.servicoRecursos.consultarCalendario(this.paraDataLocal(new Date()), 14).subscribe({
      next: (calendario) => {
        this.calendario = calendario;
        this.carregando = false;
        this.carregandoCalendario = false;
      },
      error: () => {
        this.mensagemErro = 'Não foi possível carregar o calendário de berços.';
        this.carregando = false;
        this.carregandoCalendario = false;
      }
    });
  }

  private processarEvento(evento: EventoRecursosTempoReal): void {
    if (!evento) {
      return;
    }
    if (evento.resumo) {
      this.resumo = evento.resumo;
      this.zonas = evento.resumo.zonas ?? [];
      this.equipamentos = evento.resumo.equipamentos ?? [];
    }
    if (evento.alocacao) {
      this.respostaAlocacao = evento.alocacao;
    }
    this.carregarListasDependentes();
  }

  private ajustarData(base: Date, horas: number): Date {
    const copia = new Date(base);
    copia.setHours(copia.getHours() + horas);
    copia.setMinutes(0, 0, 0);
    return copia;
  }

  private paraIsoLocal(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    const horas = String(data.getHours()).padStart(2, '0');
    const minutos = String(data.getMinutes()).padStart(2, '0');
    return `${ano}-${mes}-${dia}T${horas}:${minutos}`;
  }

  private paraDataLocal(valor: Date | string): string {
    const data = typeof valor === 'string' ? new Date(valor) : valor;
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }

  private normalizarDataHora(valor: string): string {
    const data = new Date(valor);
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    const horas = String(data.getHours()).padStart(2, '0');
    const minutos = String(data.getMinutes()).padStart(2, '0');
    const segundos = String(data.getSeconds()).padStart(2, '0');
    return `${ano}-${mes}-${dia}T${horas}:${minutos}:${segundos}`;
  }

  private paraIsoData(valor: string): string {
    return valor;
  }
}
