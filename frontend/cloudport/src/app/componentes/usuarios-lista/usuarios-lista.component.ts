import { Component } from '@angular/core';

interface UsuarioResumo {
  nome: string;
  email: string;
  status: 'Ativo' | 'Inativo';
}

@Component({
  selector: 'app-usuarios-lista',
  templateUrl: './usuarios-lista.component.html',
  styleUrls: ['./usuarios-lista.component.css']
})
export class UsuariosListaComponent {
  usuarios: UsuarioResumo[] = [
    { nome: 'Ana Silva', email: 'ana.silva@empresa.com', status: 'Ativo' },
    { nome: 'Bruno Souza', email: 'bruno.souza@empresa.com', status: 'Inativo' },
    { nome: 'Carla Lima', email: 'carla.lima@empresa.com', status: 'Ativo' }
  ];
}
