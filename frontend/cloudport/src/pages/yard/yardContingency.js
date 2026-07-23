import { useCallback, useEffect, useMemo, useState } from 'react';
import { formatError } from '../../api.js';

export const YARD_CONNECTION_STATUS = Object.freeze({
  ONLINE: 'ONLINE',
  OFFLINE: 'OFFLINE',
  RECONNECTING: 'RECONECTANDO'
});

export const YARD_SNAPSHOT_VERSION = 1;
export const YARD_SNAPSHOT_MAX_AGE_MS = 30 * 60 * 1000;
const YARD_SNAPSHOT_KEY = `cloudport:yard-map:snapshot:v${YARD_SNAPSHOT_VERSION}`;

function storageAvailable(storage) {
  return Boolean(storage && typeof storage.getItem === 'function' && typeof storage.setItem === 'function');
}

export function fingerprintYardSnapshot(data) {
  const source = JSON.stringify(data ?? null);
  let hash = 2166136261;
  for (let index = 0; index < source.length; index += 1) {
    hash ^= source.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
}

export function createYardSnapshot(data, synchronizedAt = new Date().toISOString()) {
  return {
    version: YARD_SNAPSHOT_VERSION,
    synchronizedAt,
    fingerprint: fingerprintYardSnapshot(data),
    data
  };
}

export function isYardSnapshotExpired(snapshot, now = Date.now(), maxAgeMs = YARD_SNAPSHOT_MAX_AGE_MS) {
  const synchronizedAt = Date.parse(snapshot?.synchronizedAt ?? '');
  return !Number.isFinite(synchronizedAt) || now - synchronizedAt > maxAgeMs;
}

export function readYardSnapshot(storage = globalThis.localStorage) {
  if (!storageAvailable(storage)) return null;
  try {
    const snapshot = JSON.parse(storage.getItem(YARD_SNAPSHOT_KEY) ?? 'null');
    if (snapshot?.version !== YARD_SNAPSHOT_VERSION || !snapshot?.data) return null;
    return snapshot;
  } catch {
    return null;
  }
}

export function writeYardSnapshot(data, storage = globalThis.localStorage, synchronizedAt) {
  const snapshot = createYardSnapshot(data, synchronizedAt);
  if (storageAvailable(storage)) {
    try {
      storage.setItem(YARD_SNAPSHOT_KEY, JSON.stringify(snapshot));
    } catch {
      // A indisponibilidade do armazenamento não pode interromper a operação online.
    }
  }
  return snapshot;
}

export function reconcileYardSnapshots(localSnapshot, officialData) {
  if (!localSnapshot) return { diverged: false, previousFingerprint: null, currentFingerprint: fingerprintYardSnapshot(officialData) };
  const currentFingerprint = fingerprintYardSnapshot(officialData);
  return {
    diverged: localSnapshot.fingerprint !== currentFingerprint,
    previousFingerprint: localSnapshot.fingerprint,
    currentFingerprint
  };
}

export function useYardContingency(loader, dependencies = []) {
  const initialSnapshot = useMemo(() => readYardSnapshot(), []);
  const [data, setData] = useState(initialSnapshot?.data ?? null);
  const [loading, setLoading] = useState(!initialSnapshot);
  const [error, setError] = useState('');
  const [connectionStatus, setConnectionStatus] = useState(
    globalThis.navigator?.onLine === false ? YARD_CONNECTION_STATUS.OFFLINE : YARD_CONNECTION_STATUS.RECONNECTING
  );
  const [lastSynchronization, setLastSynchronization] = useState(initialSnapshot?.synchronizedAt ?? null);
  const [snapshotExpired, setSnapshotExpired] = useState(initialSnapshot ? isYardSnapshotExpired(initialSnapshot) : false);
  const [reconciliation, setReconciliation] = useState(null);

  const reload = useCallback(async (options) => {
    const silent = options?.silent === true;
    const cachedBeforeReload = readYardSnapshot();
    if (!silent) setLoading(true);
    setConnectionStatus(YARD_CONNECTION_STATUS.RECONNECTING);
    setError('');
    try {
      const response = await loader();
      const result = reconcileYardSnapshots(cachedBeforeReload, response);
      const snapshot = writeYardSnapshot(response);
      setData(response);
      setLastSynchronization(snapshot.synchronizedAt);
      setSnapshotExpired(false);
      setReconciliation(result.diverged ? result : null);
      setConnectionStatus(YARD_CONNECTION_STATUS.ONLINE);
      return response;
    } catch (reason) {
      const cached = readYardSnapshot();
      if (cached) {
        setData(cached.data);
        setLastSynchronization(cached.synchronizedAt);
        setSnapshotExpired(isYardSnapshotExpired(cached));
      }
      setConnectionStatus(YARD_CONNECTION_STATUS.OFFLINE);
      setError(cached
        ? `Modo de contingência ativo. Dados congelados na última sincronização. Falha: ${formatError(reason)}`
        : `Sistema offline e nenhuma fotografia local está disponível. Falha: ${formatError(reason)}`);
      return undefined;
    } finally {
      setLoading(false);
    }
  }, dependencies);

  useEffect(() => { reload(); }, [reload]);

  useEffect(() => {
    const handleOffline = () => setConnectionStatus(YARD_CONNECTION_STATUS.OFFLINE);
    const handleOnline = () => {
      setConnectionStatus(YARD_CONNECTION_STATUS.RECONNECTING);
      reload({ silent: true });
    };
    globalThis.addEventListener?.('offline', handleOffline);
    globalThis.addEventListener?.('online', handleOnline);
    return () => {
      globalThis.removeEventListener?.('offline', handleOffline);
      globalThis.removeEventListener?.('online', handleOnline);
    };
  }, [reload]);

  return {
    data,
    loading,
    error,
    setError,
    reload,
    connectionStatus,
    lastSynchronization,
    snapshotExpired,
    reconciliation,
    isOffline: connectionStatus !== YARD_CONNECTION_STATUS.ONLINE,
    hasSnapshot: Boolean(data)
  };
}
