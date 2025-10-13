import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { NotificationBridgeService } from './componentes/service/notification-bridge.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'cloudport';
  showChrome = true;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private readonly translateService: TranslateService,
    private readonly notificationBridge: NotificationBridgeService
  ) {
    this.updateChromeVisibility(this.router.url);

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.updateChromeVisibility(event.urlAfterRedirects ?? event.url);
      });
  }

  ngOnInit(): void {
    this.translateService.addLangs(['pt', 'en', 'es']);
    this.translateService.setDefaultLang('pt');
    this.translateService.use('pt');
    this.notificationBridge.register();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateChromeVisibility(url: string): void {
    const normalizedUrl = url.startsWith('/') ? url : `/${url}`;
    this.showChrome = !normalizedUrl.startsWith('/login');
  }
}
