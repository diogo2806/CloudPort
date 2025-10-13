self.addEventListener('push', (event) => {
  const defaultOptions = {
    icon: 'assets/icons/bell.svg',
    badge: 'assets/icons/bell.svg'
  };
  let payload = {};
  if (event.data) {
    try {
      payload = event.data.json();
    } catch (error) {
      payload = { body: event.data.text() };
    }
  }
  const { title, ...optionPayload } = payload;
  const notificationTitle = title || 'CloudPort';
  const options = Object.assign({}, defaultOptions, optionPayload);
  event.waitUntil(self.registration.showNotification(notificationTitle, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const destino = (event.notification && event.notification.data && event.notification.data.url) || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          if ('navigate' in client && destino) {
            client.navigate(destino);
          }
          return client.focus();
        }
      }
      if (clients.openWindow && destino) {
        return clients.openWindow(destino);
      }
      return undefined;
    })
  );
});
