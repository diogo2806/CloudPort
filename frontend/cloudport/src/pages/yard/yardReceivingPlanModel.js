const TERMINAL_TRANSACTION_STATUSES = new Set([
  'CANCELADA',
  'CANCELADO',
  'CONCLUIDA',
  'CONCLUIDO',
  'FINALIZADA',
  'FINALIZADO'
]);

function normalized(value) {
  return String(value ?? '').trim().toUpperCase();
}

function sameId(left, right) {
  if (left === undefined || left === null || right === undefined || right === null) return false;
  return String(left) === String(right);
}

function findById(items, id) {
  return (items ?? []).find((item) => sameId(item?.id, id)) ?? null;
}

function operationCategory(operationType, preadviceType) {
  const operation = normalized(operationType);
  const advised = normalized(preadviceType);
  if (operation.includes('VAZIO') || advised === 'VAZIO') return 'VAZIO';
  if (operation.includes('IMPORT')) return 'IMPORTACAO';
  if (operation.includes('TRANSBORD') || operation.includes('TRANSSHIP')) return 'TRANSBORDO';
  if (operation.includes('EXPORT') || advised === 'EXPORTACAO') return 'EXPORTACAO';
  return operation || advised || 'NAO_INFORMADA';
}

function isoProfile(isoType) {
  const iso = normalized(isoType);
  let length = null;
  if (iso.startsWith('2')) length = 20;
  if (iso.startsWith('4')) length = 40;
  if (iso.startsWith('L')) length = 45;
  const reefer = iso.includes('R');
  return {
    length,
    reefer,
    equipmentType: reefer ? 'REEFER' : iso ? 'DRY' : null
  };
}

function weightInTons(weightKg) {
  if (weightKg === undefined || weightKg === null || weightKg === '') return null;
  const numeric = Number(weightKg);
  return Number.isFinite(numeric) ? numeric / 1000 : null;
}

function referenceLabel({ booking, billOfLading, order, preadvice }) {
  const labels = [
    booking?.codigo ? `Booking ${booking.codigo}` : null,
    billOfLading?.numero ? `BL ${billOfLading.numero}` : null,
    order?.codigo ? `${order.tipo || 'Ordem'} ${order.codigo}` : null,
    preadvice?.codigo ? `Pré-aviso ${preadvice.codigo}` : null
  ].filter(Boolean);
  return labels.join(' · ') || 'Sem referência comercial';
}

function validateReferences(category, context) {
  const blockers = [];
  if (category === 'IMPORTACAO' && !context.billOfLading && !context.order) {
    blockers.push('Importação sem Bill of Lading ou ordem vinculada.');
  }
  if (category === 'EXPORTACAO' && !context.booking && !context.order && !context.preadvice) {
    blockers.push('Exportação sem booking, ordem ou pré-aviso vinculado.');
  }
  if (category === 'VAZIO' && !context.booking && !context.order && !context.preadvice) {
    blockers.push('Movimento de vazio sem booking, ordem ou pré-aviso vinculado.');
  }
  return blockers;
}

export function buildReceivingRows(visit, references = {}, complements = {}) {
  if (!visit) return [];

  const bookings = references.bookings ?? [];
  const orders = references.ordens ?? [];
  const preadvices = references.preAvisos ?? [];
  const billsOfLading = complements.billsOfLading ?? references.billsOfLading ?? [];

  return (visit.transacoes ?? []).map((transaction) => {
    const preadvice = findById(preadvices, transaction.preadviceId);
    const order = findById(orders, transaction.orderId ?? preadvice?.orderId);
    const booking = findById(bookings, transaction.bookingId ?? preadvice?.bookingId ?? order?.bookingId);
    const billOfLading = findById(billsOfLading, transaction.billOfLadingId ?? order?.billOfLadingId);
    const unit = transaction.unidadeReferencia || preadvice?.unidadeReferencia || order?.unidadeReferencia || '';
    const category = operationCategory(transaction.tipoOperacao, preadvice?.tipo);
    const iso = isoProfile(preadvice?.isoType);
    const blockers = [];
    const warnings = [];

    if (!unit.trim()) blockers.push('Transação sem unidade identificada.');
    if (transaction.troubleAtivo) blockers.push('Transação possui trouble ativo.');
    if (TERMINAL_TRANSACTION_STATUSES.has(normalized(transaction.status))) {
      blockers.push(`Transação em estado final ${transaction.status}.`);
    }
    blockers.push(...validateReferences(category, { booking, billOfLading, order, preadvice }));

    if (!preadvice?.isoType) warnings.push('ISO type não informado; comprimento e tipo serão validados antes da posição definitiva.');
    if (preadvice?.pesoBrutoKg === undefined || preadvice?.pesoBrutoKg === null) {
      warnings.push('Peso bruto não informado; validar limite da pilha antes do recebimento.');
    }

    const armador = booking?.armador || billOfLading?.armador || null;
    const outboundVisit = booking?.viagem || billOfLading?.viagem || null;

    return {
      id: transaction.id,
      transaction,
      booking,
      billOfLading,
      order,
      preadvice,
      unit,
      category,
      eligible: blockers.length === 0,
      blockers,
      warnings,
      referenceLabel: referenceLabel({ booking, billOfLading, order, preadvice }),
      container: {
        id: transaction.id,
        codigo: unit,
        etaChegada: visit.checkinEm || visit.iniciadoEm || null,
        etaPartida: booking?.validadeFim || billOfLading?.validadeFim || order?.validadeFim || null,
        pesoToneladas: weightInTons(preadvice?.pesoBrutoKg),
        tipoCarga: category,
        destino: 'A_DEFINIR_NO_DISPATCH',
        restricoes: warnings.join(' '),
        categoria: category,
        armador,
        visitaSaida: outboundVisit,
        comprimentoPes: iso.length,
        tipoEquipamento: iso.equipmentType,
        estadoCarga: category === 'VAZIO' ? 'VAZIO' : 'CHEIO',
        refrigerado: iso.reefer,
        perigoso: false,
        classeImo: null,
        numeroOnu: null
      }
    };
  });
}

export function countReceivingRows(rows) {
  return (rows ?? []).reduce((summary, row) => {
    summary.total += 1;
    if (row.eligible) summary.eligible += 1;
    else summary.blocked += 1;
    if (row.warnings?.length) summary.withWarnings += 1;
    return summary;
  }, { total: 0, eligible: 0, blocked: 0, withWarnings: 0 });
}
