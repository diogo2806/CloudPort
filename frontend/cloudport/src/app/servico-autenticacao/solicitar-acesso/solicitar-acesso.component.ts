import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-solicitar-acesso',
  templateUrl: './solicitar-acesso.component.html',
  styleUrls: ['./solicitar-acesso.component.css']
})
export class SolicitarAcessoComponent {
  cpf: string;
  dataNascimento: Date;
  nome: string;
  perfilAcesso: string;
  celular: string;
  email: string;
  cnpj: string;
  razaoSocial: string;
  tipoDocumento: string;
  numeroDocumento: string;
  dataEmissao: Date;
  orgaoEmissor: string;
  validade: Date;

  constructor(private router: Router) {
    this.cpf = '';
    this.dataNascimento = new Date();
    this.nome = '';
    this.perfilAcesso = '';
    this.celular = '';
    this.email = '';
    this.cnpj = '';
    this.razaoSocial = '';
    this.tipoDocumento = '';
    this.numeroDocumento = '';
    this.dataEmissao = new Date();
    this.orgaoEmissor = '';
    this.validade = new Date();
  }

  onSubmit(): void {
    console.log(`CPF: ${this.cpf}, Data de Nascimento: ${this.dataNascimento}, Nome: ${this.nome}, Perfil de Acesso: ${this.perfilAcesso}, Celular: ${this.celular}, Email: ${this.email}, CNPJ: ${this.cnpj}, Razão Social: ${this.razaoSocial}, Tipo de Documento: ${this.tipoDocumento}, Número do Documento: ${this.numeroDocumento}, Data de Emissão: ${this.dataEmissao}, Orgão Emissor: ${this.orgaoEmissor}, Validade: ${this.validade}`);
    // Aqui, você pode adicionar a lógica para lidar com a submissão do formulário.
    // Por exemplo, enviar os dados para o servidor ou passar os dados para outra parte do seu aplicativo.
  }

  voltarParaLogin(): void {
    this.router.navigate(['/login']);
  }
}
