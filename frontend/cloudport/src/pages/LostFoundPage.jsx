import { PageHeader } from '../components.jsx';
import { LostFoundQueue } from './LostFoundQueue.jsx';

export function LostFoundPage() {
  return <>
    <PageHeader eyebrow="Pátio" title="Unidades de carga não localizadas" description="Investigação, identificação, associação, regularização, baixa e encerramento de unidades sem registro ou não localizadas." />
    <LostFoundQueue />
  </>;
}
