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
        .catch(() => null);
    }
  }

  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false;
    }
    const permission = await Notification.requestPermission();
    return permission === 'granted';
  }

  async showNotification(title: string, options?: NotificationOptions): Promise<void> {
    if (!('Notification' in window)) {
      return;
    }
    const permission = Notification.permission === 'granted'
      ? 'granted'
      : await Notification.requestPermission();
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
