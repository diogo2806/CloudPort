import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ServicoSimuladorPatio } from '../../service/servico-simulador-patio/servico-simulador-patio.service';
import { SanitizadorConteudoService } from '../../service/sanitizacao/sanitizador-conteudo.service';

interface FormularioSimulacao {
  tipoCenario: FormControl<string>;
  horasAtraso: FormControl<number | null>;
  codigoEquipamento: FormControl<string>;
  quantidadeConteineres: FormControl<number | null>;
  descricao: FormControl<string>;
}

@Component({
  selector: 'app-simulador-patio',
  templateUrl: './simulador-patio.component.html',
  styleUrls: ['./simulador-patio.component.css'],
  standalone: false
})
export class SimuladorPatioComponent implements OnInit {
  formularioSimulacao: FormGroup<FormularioSimulacao>;
  resultado: any = null;
  carregando = false;
  erro?: string;
  tiposCenario = [
    { valor: 'ATRASO_NAVIO', label: '🚢 Atraso de Navio' },
    { valor: 'MANUTENCAO_EQUIPAMENTO', label: '🔧 Manutenção de Equipamento' },
    { valor: 'AUMENTO_VOLUME', label: '📦 Aumento de Volume' }
  ];

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly servicoSimulador: ServicoSimuladorPatio,
    private readonly sanitizador: SanitizadorConteudoService
  ) {
    this.formularioSimulacao = this.formBuilder.group({
      tipoCenario: this.formBuilder.control('ATRASO_NAVIO', { validators: [Validators.required], nonNullable: true }),
      horasAtraso: this.formBuilder.control<number | null>(4),
      codigoEquipamento: this.formBuilder.control('RTG-01', { nonNullable: true }),
      quantidadeConteineres: this.formBuilder.control<number | null>(100),
      descricao: this.formBuilder.control('', { nonNullable: true })
    });
  }

  ngOnInit(): void {
    this.atualizarDescricao();
    this.formularioSimulacao.get('tipoCenario')?.valueChanges.subscribe(() => {
      this.atualizarDescricao();
    });
  }

  atualizarDescricao(): void {
    const tipo = this.formularioSimulacao.get('tipoCenario')?.value;
    let descricao = '';
    switch (tipo) {
      case 'ATRASO_NAVIO':
        descricao = `Navio atrasa ${this.formularioSimulacao.get('horasAtraso')?.value || 4} horas`;
        break;
      case 'MANUTENCAO_EQUIPAMENTO':
        descricao = `${this.formularioSimulacao.get('codigoEquipamento')?.value} entra em manutenção`;
        break;
      case 'AUMENTO_VOLUME':
        descricao = `Volume aumenta em ${this.formularioSimulacao.get('quantidadeConteineres')?.value || 100} contêineres`;
        break;
    }
    this.formularioSimulacao.patchValue({ descricao });
  }

  executarSimulacao(): void {
    if (this.formularioSimulacao.invalid) {
      this.erro = 'Preencha os dados obrigatórios.';
      return;
    }

    const valor = this.formularioSimulacao.getRawValue();
    const cenario = {
      tipoCenario: valor.tipoCenario,
      descricao: valor.descricao,
      horasAtraso: valor.horasAtraso,
      codigoEquipamento: valor.codigoEquipamento,
      quantidadeConteinoresAdicionais: valor.quantidadeConteineres
    };

    this.carregando = true;
    this.erro = undefined;

    this.servicoSimulador.simularCenario(cenario).subscribe({
      next: (resultado) => {
        this.resultado = resultado;
        this.carregando = false;
      },
      error: (erro) => {
        this.erro = `Erro ao simular: ${erro?.error?.message || 'falha desconhecida'}`;
        this.carregando = false;
      }
    });
  }

  obterCorDelta(valor: number): string {
    if (valor > 15) return '#dc2626';
    if (valor > 5) return '#f59e0b';
    return '#10b981';
  }

  obterCorImpacto(impacto: string): string {
    if (impacto.includes('CRÍTICO')) return '#dc2626';
    if (impacto.includes('ALTO')) return '#f59e0b';
    return '#10b981';
  }

  sanitizar(texto: string | null | undefined): string {
    return this.sanitizador.sanitizar(texto ?? '');
  }

  limparResultado(): void {
    this.resultado = null;
  }
}
