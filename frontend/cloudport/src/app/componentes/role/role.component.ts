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

  roleName: string = "";
  roles: any[] = []; // replace any with your actual role type

  constructor(private http: HttpClient, private authenticationService: AuthenticationService) { }

  ngOnInit() {
    this.loadRoles();
  }

  loadRoles() {
    const token = this.authenticationService.currentUserValue?.token;

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };

    this.http.get<any[]>(`${environment.role.list}`, httpOptions)
      .subscribe(response => {
        this.roles = response;
      });
  }

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
        console.log('Role created:', response);
        this.loadRoles(); // Reload the list of roles after a new role is created
      });
  }
}
