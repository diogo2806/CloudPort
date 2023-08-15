import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy } from '@angular/router';

export class CustomReuseStrategy implements RouteReuseStrategy {
  private handlers: { [key: string]: DetachedRouteHandle } = {};

  // Adicione um conjunto para armazenar as rotas marcadas para destruição
  private routesToDestroy: Set<string> = new Set();

  // Adicione um método para marcar uma rota para destruição
  markForDestruction(path: string): void {
    this.routesToDestroy.add(path);
  }

  shouldDetach(route: ActivatedRouteSnapshot): boolean {
    return true;
  }

  // Modifique o método store para não armazenar rotas marcadas para destruição
  store(route: ActivatedRouteSnapshot, handle: DetachedRouteHandle): void {
    if (route.routeConfig && route.routeConfig.path) {
      if (this.routesToDestroy.has(route.routeConfig.path)) {
        // Se a rota estiver marcada para destruição, não a armazene
        this.routesToDestroy.delete(route.routeConfig.path);
      } else {
        this.handlers[route.routeConfig.path] = handle;
      }
    }
  }

  shouldAttach(route: ActivatedRouteSnapshot): boolean {
    return route.routeConfig && route.routeConfig.path ? !!this.handlers[route.routeConfig.path] : false;
  }

  retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    return route.routeConfig && route.routeConfig.path ? this.handlers[route.routeConfig.path] : null;
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
    return future.
    routeConfig === curr.routeConfig;
  }

  clearHandlers() {
    this.handlers = {};
  }
  
}
