import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../service/endpoint';
import { AuthenticationService } from '../service/servico-autenticacao/authentication.service';
import { HttpHeaders } from '@angular/common/http';


@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.css']
})
export class RoleComponent {

  roleName: string = "";

  constructor(private http: HttpClient, private authenticationService: AuthenticationService) { }

  createRole() {

    const token = this.authenticationService.currentUserValue?.token; // Replace with your actual token

    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type':  'application/json',
        'Authorization': 'Bearer ' + token
      })
    };


    this.http.post(`${environment.role.create}`, { name: this.roleName }, httpOptions)
      .subscribe(response => {
        console.log('Role created:', response);
      });
  }
}
