import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PushNotificationService {
  private registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;

  constructor() {
    if ('serviceWorker' in navigator) {
      this.registrationPromise = navigator.serviceWorker
        .register('/assets/sw.js')
        .then(() => navigator.serviceWorker.ready)
        .catch(async () => {
          try {
            return await navigator.serviceWorker.ready;
          } catch {
            return null;
          }
        });
    }
  }

  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false;
    }
    try {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    } catch {
      return false;
    }
  }

  async showNotification(title: string, options?: NotificationOptions): Promise<void> {
    if (!('Notification' in window)) {
      return;
    }
    let permission: NotificationPermission;
    if (Notification.permission === 'granted') {
      permission = 'granted';
    } else {
      try {
        permission = await Notification.requestPermission();
      } catch {
        permission = 'denied';
      }
    }
    if (permission !== 'granted') {
      throw new Error('notification-permission-denied');
    }
    const registration = await this.registrationPromise;
    if (registration) {
      await registration.showNotification(title, options);
    } else {
      new Notification(title, options);
    }
  }
}
