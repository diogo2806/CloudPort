import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../service/endpoint';

@Component({
  selector: 'app-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.css']
})
export class RoleComponent {

  roleName: string = "";

  constructor(private http: HttpClient) { }

  createRole() {
    this.http.post(`${environment.role.create}`, { name: this.roleName })
      .subscribe(response => {
        console.log('Role created:', response);
      });
  }
}
