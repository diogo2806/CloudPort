self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('message', (event) => {
  if (!event.data || event.data.type !== 'SHOW_NOTIFICATION') {
    return;
  }
  const { title, options } = event.data;
  self.registration.showNotification(title, options || {});
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then((clients) => {
      const first = clients.find((client) => 'focus' in client);
      if (first) {
        return first.focus();
      }
      return self.clients.openWindow('/');
    })
  );
});
