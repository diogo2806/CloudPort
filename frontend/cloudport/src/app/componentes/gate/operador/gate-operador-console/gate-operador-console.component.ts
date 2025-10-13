import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { map, Observable, of, tap } from 'rxjs';
import {
  GateBloqueioRequest,
  GateLiberacaoManualRequest,
  GateOcorrenciaRequest,
  GateOperadorEvento,
  GateOperadorExcecao,
  GateOperadorPainel,
  GateOperadorVeiculo
} from '../../../model/gate/operador.model';
import { GateOperadorService } from '../../../service/servico-gate/gate-operador.service';
import { GateEnumOption } from '../../../model/gate/agendamento.model';

interface AlertaTemporario {
  evento: GateOperadorEvento;
  expiraEm: number;
}

@Component({
  selector: 'app-gate-operador-console',
  templateUrl: './gate-operador-console.component.html',
  styleUrls: ['./gate-operador-console.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GateOperadorConsoleComponent implements OnInit, OnDestroy {
  readonly painel$: Observable<GateOperadorPainel> = this.gateOperadorService.painel$;
  readonly eventos$: Observable<GateOperadorEvento[]> = this.gateOperadorService.eventos$;
  readonly canaisEntrada$: Observable<GateEnumOption[]> = this.gateOperadorService.listarCanaisEntrada();
  readonly motivosBloqueio$: Observable<GateEnumOption[]> = this.gateOperadorService.listarMotivosExcecao();
  readonly tiposOcorrencia$: Observable<GateEnumOption[]> = this.gateOperadorService.listarTiposOcorrencia();
  readonly statusConexao$ = this.gateOperadorService.statusConexao$;

  readonly filaResumo$: Observable<{ titulo: string; dados: GateOperadorVeiculo[] }[]> = this.painel$.pipe(
    map((painel) => [
      { titulo: 'Entrada', dados: painel.filasEntrada.flatMap((fila) => fila.veiculos) },
      { titulo: 'Saída', dados: painel.filasSaida.flatMap((fila) => fila.veiculos) }
    ])
  );

  readonly formLiberacao = this.fb.group({
    canalEntrada: [null, Validators.required],
    justificativa: ['', [Validators.required, Validators.minLength(5)]],
    notificarTransportadora: [true]
  });

  readonly formBloqueio = this.fb.group({
    motivoCodigo: [null, Validators.required],
    justificativa: ['', [Validators.required, Validators.minLength(5)]],
    bloqueioAte: ['', Validators.required]
  });

  readonly formOcorrencia = this.fb.group({
    tipoCodigo: [null, Validators.required],
    descricao: ['', [Validators.required, Validators.minLength(5)]],
    veiculoId: [null]
  });

  modalLiberacaoAberto = false;
  modalBloqueioAberto = false;
  modalOcorrenciaAberto = false;
  veiculoSelecionado: GateOperadorVeiculo | null = null;
  mensagemFeedback: string | null = null;
  erroAcao: string | null = null;

  alertasRecentes$: Observable<GateOperadorEvento[]> = of([]);

  private readonly alertasBuffer: AlertaTemporario[] = [];

  constructor(private readonly gateOperadorService: GateOperadorService, private readonly fb: FormBuilder) {}

  ngOnInit(): void {
    this.gateOperadorService.carregarPainel().subscribe({
      error: (erro) => console.warn('Não foi possível carregar o painel do Gate.', erro)
    });
    this.gateOperadorService.atualizarHistorico().subscribe({
      error: (erro) => console.warn('Não foi possível carregar o histórico inicial do Gate.', erro)
    });
    this.gateOperadorService.conectarEventos();
    this.alertasRecentes$ = this.gateOperadorService.alertas$.pipe(
      tap((evento) => this.empilharAlerta(evento)),
      map(() => this.obterAlertasAtivos())
    );
  }

  ngOnDestroy(): void {
    this.gateOperadorService.desconectarEventos();
  }

  abrirModalLiberacao(veiculo: GateOperadorVeiculo): void {
    this.veiculoSelecionado = veiculo;
    this.formLiberacao.reset({ canalEntrada: null, justificativa: '', notificarTransportadora: true });
    this.modalLiberacaoAberto = true;
    this.mensagemFeedback = null;
    this.erroAcao = null;
  }

  abrirModalBloqueio(veiculo: GateOperadorVeiculo): void {
    this.veiculoSelecionado = veiculo;
    this.formBloqueio.reset({ motivoCodigo: null, justificativa: '', bloqueioAte: '' });
    this.modalBloqueioAberto = true;
    this.mensagemFeedback = null;
    this.erroAcao = null;
  }

  abrirModalOcorrencia(veiculo: GateOperadorVeiculo | null): void {
    this.veiculoSelecionado = veiculo;
    this.formOcorrencia.reset({ tipoCodigo: null, descricao: '', veiculoId: veiculo?.id ?? null });
    this.modalOcorrenciaAberto = true;
    this.mensagemFeedback = null;
    this.erroAcao = null;
  }

  fecharModais(): void {
    this.modalLiberacaoAberto = false;
    this.modalBloqueioAberto = false;
    this.modalOcorrenciaAberto = false;
  }

  confirmarLiberacao(): void {
    if (!this.veiculoSelecionado || this.formLiberacao.invalid) {
      this.formLiberacao.markAllAsTouched();
      return;
    }

    const payload = this.formLiberacao.getRawValue() as GateLiberacaoManualRequest;
    this.mensagemFeedback = 'Enviando liberação manual...';
    this.erroAcao = null;

    this.gateOperadorService.liberarVeiculo(this.veiculoSelecionado.id, payload).subscribe({
      next: () => {
        this.mensagemFeedback = 'Veículo liberado com sucesso.';
        this.fecharModais();
      },
      error: () => {
        this.erroAcao = 'Não foi possível liberar o veículo. Tente novamente em instantes.';
      }
    });
  }

  confirmarBloqueio(): void {
    if (!this.veiculoSelecionado || this.formBloqueio.invalid) {
      this.formBloqueio.markAllAsTouched();
      return;
    }

    const payload = this.formBloqueio.getRawValue() as GateBloqueioRequest;
    this.mensagemFeedback = 'Registrando bloqueio do veículo...';
    this.erroAcao = null;

    this.gateOperadorService.bloquearVeiculo(this.veiculoSelecionado.id, payload).subscribe({
      next: () => {
        this.mensagemFeedback = 'Bloqueio registrado com sucesso.';
        this.fecharModais();
      },
      error: () => {
        this.erroAcao = 'Não foi possível bloquear o veículo. Verifique os dados e tente novamente.';
      }
    });
  }

  confirmarOcorrencia(): void {
    if (this.formOcorrencia.invalid) {
      this.formOcorrencia.markAllAsTouched();
      return;
    }

    const payload = this.formOcorrencia.getRawValue() as GateOcorrenciaRequest;
    this.mensagemFeedback = 'Enviando ocorrência ao time de monitoramento...';
    this.erroAcao = null;

    this.gateOperadorService.registrarOcorrencia(payload).subscribe({
      next: () => {
        this.mensagemFeedback = 'Ocorrência registrada com sucesso.';
        this.fecharModais();
      },
      error: () => {
        this.erroAcao = 'Não foi possível registrar a ocorrência. Revise as informações e tente novamente.';
      }
    });
  }

  imprimirComprovante(veiculo: GateOperadorVeiculo): void {
    if (!veiculo.podeImprimirComprovante) {
      return;
    }

    this.mensagemFeedback = 'Preparando comprovante de gate...';
    this.gateOperadorService.imprimirComprovante(veiculo.id).subscribe({
      next: () => {
        this.mensagemFeedback = 'Comprovante enviado para impressão.';
      },
      error: () => {
        this.erroAcao = 'Não foi possível imprimir o comprovante. Tente novamente.';
      }
    });
  }

  possuiExcecaoCritica(veiculo: GateOperadorVeiculo | null | undefined): boolean {
    return (veiculo?.excecoes ?? []).some((excecao) => (excecao.nivel ?? '').toUpperCase() === 'CRITICA');
  }

  possuiExcecaoAlerta(veiculo: GateOperadorVeiculo | null | undefined): boolean {
    return (veiculo?.excecoes ?? []).some((excecao) => (excecao.nivel ?? '').toUpperCase() === 'ALERTA');
  }

  classeExcecao(excecao: GateOperadorExcecao): string {
    const nivel = (excecao.nivel ?? '').toUpperCase();
    if (nivel === 'CRITICA') {
      return 'excecao-badge excecao-badge-critica';
    }
    if (nivel === 'ALERTA') {
      return 'excecao-badge excecao-badge-alerta';
    }
    return 'excecao-badge';
  }

  contatoLink(tipo: string, valor: string): string {
    const tipoUpper = (tipo || '').toUpperCase();
    if (tipoUpper === 'TELEFONE' || tipoUpper === 'WHATSAPP') {
      const numero = valor.replace(/[^0-9+]/g, '');
      return `tel:${numero}`;
    }
    if (tipoUpper === 'EMAIL') {
      return `mailto:${valor}`;
    }
    return valor;
  }

  contatoDescricao(tipo: string): string {
    const tipoUpper = (tipo || '').toUpperCase();
    if (tipoUpper === 'TELEFONE') {
      return 'Ligar';
    }
    if (tipoUpper === 'WHATSAPP') {
      return 'WhatsApp';
    }
    if (tipoUpper === 'EMAIL') {
      return 'E-mail';
    }
    return tipo;
  }

  private empilharAlerta(evento: GateOperadorEvento): void {
    const expiracao = Date.now() + 15000;
    this.alertasBuffer.unshift({ evento, expiraEm: expiracao });
    this.descartarAlertasExpirados();
  }

  private obterAlertasAtivos(): GateOperadorEvento[] {
    this.descartarAlertasExpirados();
    return this.alertasBuffer.map((item) => item.evento);
  }

  private descartarAlertasExpirados(): void {
    const agora = Date.now();
    for (let index = this.alertasBuffer.length - 1; index >= 0; index -= 1) {
      if (this.alertasBuffer[index].expiraEm < agora) {
        this.alertasBuffer.splice(index, 1);
      }
    }
  }
}
