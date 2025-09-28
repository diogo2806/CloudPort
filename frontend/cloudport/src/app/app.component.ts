import { Component, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnDestroy {
  title = 'cloudport';
  showChrome = true;
  private readonly destroy$ = new Subject<void>();

  constructor(private router: Router) {
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateChromeVisibility(url: string): void {
    const normalizedUrl = url.startsWith('/') ? url : `/${url}`;
    this.showChrome = !normalizedUrl.startsWith('/login');
  }
}
