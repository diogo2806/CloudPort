import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import { ServicoFerroviaService, VisitaTrem } from '../../../service/servico-ferrovia/servico-ferrovia.service';

@Component({
  selector: 'app-importacao-manifesto-visita',
  templateUrl: './importacao-manifesto-visita.component.html',
  styleUrls: ['./importacao-manifesto-visita.component.css']
})
export class ImportacaoManifestoVisitaComponent {
  arquivoSelecionado?: File;
  nomeArquivoSeguro = '';
  mensagemErro?: string;
  mensagemSucesso?: string;
  carregando = false;
  visitaImportada?: VisitaTrem;
  @ViewChild('campoArquivo') campoArquivo?: ElementRef<HTMLInputElement>;

  constructor(
    private readonly servicoFerrovia: ServicoFerroviaService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService,
    private readonly router: Router
  ) {}

  aoSelecionarArquivo(evento: Event): void {
    const elemento = evento.target as HTMLInputElement | null;
    const arquivo = elemento?.files?.[0];
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    this.visitaImportada = undefined;

    if (!arquivo) {
      this.arquivoSelecionado = undefined;
      this.nomeArquivoSeguro = '';
      return;
    }

    const mensagemValidacao = this.validarArquivo(arquivo);
    if (mensagemValidacao) {
      this.mensagemErro = mensagemValidacao;
      this.arquivoSelecionado = undefined;
      this.nomeArquivoSeguro = '';
      if (elemento) {
        elemento.value = '';
      }
      return;
    }

    this.arquivoSelecionado = arquivo;
    this.nomeArquivoSeguro = this.sanitizadorConteudo.sanitizar(arquivo.name);
  }

  importarManifesto(): void {
    if (!this.arquivoSelecionado) {
      this.mensagemErro = 'Selecione um arquivo antes de importar.';
      this.mensagemSucesso = undefined;
      return;
    }

    this.carregando = true;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;

    this.servicoFerrovia
      .importarManifestoVisita(this.arquivoSelecionado)
      .pipe(finalize(() => (this.carregando = false)))
      .subscribe({
        next: (visita) => {
          this.visitaImportada = visita;
          this.mensagemSucesso = 'Manifesto importado com sucesso.';
        },
        error: (erro) => {
          const mensagem = erro?.message ?? 'Não foi possível importar o manifesto.';
          this.mensagemErro = this.sanitizadorConteudo.sanitizar(mensagem) || 'Não foi possível importar o manifesto.';
        }
      });
  }

  cancelarSelecao(): void {
    this.arquivoSelecionado = undefined;
    this.nomeArquivoSeguro = '';
    this.visitaImportada = undefined;
    this.mensagemErro = undefined;
    this.mensagemSucesso = undefined;
    if (this.campoArquivo?.nativeElement) {
      this.campoArquivo.nativeElement.value = '';
    }
  }

  voltarParaLista(): void {
    this.router.navigate(['/home', 'ferrovia', 'visitas']);
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  private validarArquivo(arquivo: File): string | null {
    if (!(arquivo instanceof File)) {
      return 'Selecione um arquivo válido.';
    }
    if (arquivo.size === 0) {
      return 'O arquivo selecionado está vazio.';
    }
    const nomeSanitizado = this.sanitizadorConteudo.sanitizar(arquivo.name);
    if (!nomeSanitizado) {
      return 'O nome do arquivo selecionado é inválido.';
    }
    if (nomeSanitizado !== arquivo.name) {
      return 'O nome do arquivo contém caracteres não permitidos.';
    }
    return null;
  }
}
