import { PageHeader } from '../components.jsx';
import { LostFoundQueue } from './LostFoundQueue.jsx';

export function LostFoundPage() {
  return <>
    <PageHeader eyebrow="Pátio" title="Lost & Found / TBD" description="Investigação, associação, regularização, baixa e encerramento de unidades desconhecidas ou não localizadas." />
    <LostFoundQueue />
  </>;
}
