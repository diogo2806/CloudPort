import { Component, OnDestroy, OnInit } from '@angular/core';
import { ServicoAutenticacao } from '../service/servico-autenticacao/servico-autenticacao.service';
import { Router, ActivatedRoute, RouteReuseStrategy } from '@angular/router';
import { CustomReuseStrategy } from '../tab-content/customreusestrategy';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {

  userToken: string = '';
  mostrarMenu: boolean = false;
  private menuStatusSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private servicoAutenticacao: ServicoAutenticacao,
    private reuseStrategy: RouteReuseStrategy 
  ) {
    let usuarioAtual: any = this.servicoAutenticacao.obterUsuarioAtual();
    if (usuarioAtual && usuarioAtual.token) {
      this.userToken = usuarioAtual.token;
    }
  }


  ngOnInit(): void {
    this.mostrarMenu = this.servicoAutenticacao.obterStatusMenuAtual();
    this.menuStatusSubscription = this.servicoAutenticacao.statusMenuObservavel.subscribe(status => {
      this.mostrarMenu = status;
    });
  }

  ngOnDestroy(): void {
    this.menuStatusSubscription?.unsubscribe();
  }


  logout() {
    this.servicoAutenticacao.encerrarSessao();
    (this.reuseStrategy as CustomReuseStrategy).markForDestruction('login'.toLowerCase());
    this.router.navigate(['login']);
  }


}
