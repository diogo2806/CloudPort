import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../service/endpoint';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';

@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.css']
})
export class RoleComponent implements OnInit {

  // Nome do papel
  roleName: string = "";

  // Lista dos papeis
  roles: any[] = [];

  constructor(private http: HttpClient, private authenticationService: AuthenticationService) { }

  // Método executado quando o componente é inicializado
  ngOnInit() {
    this.loadRoles();
  }

  // Carrega os papeis do servidor
  loadRoles() {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.get<any[]>(`${environment.role.getAll}`, httpOptions)
      .subscribe(response => {
        this.roles = response;
      });
  }

  // Cria um novo papel
  createRole() {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.post(`${environment.role.create}`, { name: this.roleName }, httpOptions)
      .subscribe(response => {
        console.log('Papel criado:', response);
        this.loadRoles();
      });
  }

  // Edita um papel existente
  editRole(roleId: number) {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.put(`${environment.role.update(roleId)}`, { name: this.roleName }, httpOptions)
        .subscribe(response => {
            console.log('Papel atualizado:', response);
            this.loadRoles();
        });
  }

  // Desativa um papel
  deactivateRole(roleId: number) {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.delete(`${environment.role.delete(roleId)}`, httpOptions)
        .subscribe(response => {
            console.log('Papel desativado:', response);
            this.loadRoles();
        });
  }
}
