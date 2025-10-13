import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationBridgeService {
  private registrationPromise: Promise<ServiceWorkerRegistration> | null = null;
  private registrationError = false;

  register(): void {
    if (!('serviceWorker' in navigator) || this.registrationPromise || this.registrationError) {
      return;
    }

    this.registrationPromise = navigator.serviceWorker
      .register('assets/sw.js')
      .catch(() => {
        this.registrationError = true;
        throw new Error('Falha ao registrar service worker');
      });
  }

  async notify(title: string, options?: NotificationOptions): Promise<void> {
    if (!('Notification' in window)) {
      return;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return;
    }

    try {
      const registration = await (this.registrationPromise ?? navigator.serviceWorker.ready);
      if (registration?.active) {
        registration.active.postMessage({ type: 'SHOW_NOTIFICATION', title, options });
      } else {
        registration.showNotification(title, options);
      }
    } catch (error) {
      console.warn('Não foi possível emitir notificação', error);
      new Notification(title, options);
    }
  }
}
