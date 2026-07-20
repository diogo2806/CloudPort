const TERMINAL_TRANSACTION_STATUSES = new Set([
  'CANCELADA',
  'CANCELADO',
  'CONCLUIDA',
  'CONCLUIDO',
  'FINALIZADA',
  'FINALIZADO'
]);

const BOOKING_STATUSES = new Set(['ABERTO', 'PARCIAL']);
const BILL_OF_LADING_STATUSES = new Set(['ATIVO', 'PARCIAL']);
const ORDER_STATUSES = new Set(['ATIVA']);
const PREADVICE_STATUSES = new Set(['ATIVO']);

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

function timestamp(value) {
  if (value instanceof Date) return value.getTime();
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (!value) return null;
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function validateStatus(label, reference, allowedStatuses) {
  if (!reference) return [];
  const status = normalized(reference.status);
  if (!status) return [`${label} sem status operacional.`];
  return allowedStatuses.has(status) ? [] : [`${label} em status ${reference.status} não permite planejamento.`];
}

function validateValidity(label, reference, now) {
  if (!reference) return [];
  const blockers = [];
  const start = timestamp(reference.validadeInicio);
  const end = timestamp(reference.validadeFim);
  if (start !== null && now < start) blockers.push(`${label} ainda não está vigente.`);
  if (end !== null && now > end) blockers.push(`${label} está expirado.`);
  return blockers;
}

function validateBalance(label, reference, totalField, usedField) {
  if (!reference) return [];
  const total = Number(reference[totalField]);
  const used = Number(reference[usedField]);
  if (!Number.isFinite(total) || !Number.isFinite(used)) return [`${label} sem saldo verificável.`];
  return used < total ? [] : [`${label} sem saldo disponível.`];
}

function validateCarrier(label, reference, visit) {
  if (!reference || reference.transportadoraId === undefined || reference.transportadoraId === null) return [];
  if (visit?.transportadoraId === undefined || visit?.transportadoraId === null) {
    return [`${label} possui transportadora, mas a truck visit não informa a transportadora responsável.`];
  }
  return sameId(reference.transportadoraId, visit.transportadoraId)
    ? []
    : [`${label} pertence a outra transportadora.`];
}

function validateReferenceId(label, id, reference) {
  return id !== undefined && id !== null && !reference
    ? [`${label} vinculado não foi encontrado no catálogo do Gate.`]
    : [];
}

function validateReferences(category, context, visit, now) {
  const blockers = [];
  const { transaction, booking, billOfLading, order, preadvice } = context;

  blockers.push(...validateReferenceId('Booking', transaction.bookingId, booking));
  blockers.push(...validateReferenceId('Bill of Lading', transaction.billOfLadingId, billOfLading));
  blockers.push(...validateReferenceId('Ordem', transaction.orderId, order));
  blockers.push(...validateReferenceId('Pré-aviso', transaction.preadviceId, preadvice));

  if (category === 'IMPORTACAO' && !billOfLading && !order) {
    blockers.push('Importação sem Bill of Lading ou ordem vinculada.');
  }
  if (category === 'EXPORTACAO' && !booking && !order && !preadvice) {
    blockers.push('Exportação sem booking, ordem ou pré-aviso vinculado.');
  }
  if (category === 'VAZIO' && !booking && !order && !preadvice) {
    blockers.push('Movimento de vazio sem booking, ordem ou pré-aviso vinculado.');
  }

  blockers.push(...validateStatus('Booking', booking, BOOKING_STATUSES));
  blockers.push(...validateValidity('Booking', booking, now));
  blockers.push(...validateBalance('Booking', booking, 'quantidadeTotal', 'quantidadeUtilizada'));
  blockers.push(...validateCarrier('Booking', booking, visit));

  blockers.push(...validateStatus('Bill of Lading', billOfLading, BILL_OF_LADING_STATUSES));
  blockers.push(...validateValidity('Bill of Lading', billOfLading, now));
  blockers.push(...validateBalance('Bill of Lading', billOfLading, 'quantidadeTotal', 'quantidadeLiberada'));

  blockers.push(...validateStatus('Ordem', order, ORDER_STATUSES));
  blockers.push(...validateValidity('Ordem', order, now));
  blockers.push(...validateCarrier('Ordem', order, visit));

  blockers.push(...validateStatus('Pré-aviso', preadvice, PREADVICE_STATUSES));
  return blockers;
}

function accessRuleBlockers(visit, rules, now) {
  if (!visit) return [];
  const referencesByScope = {
    MOTORISTA: visit.motoristaId,
    TRANSPORTADORA: visit.transportadoraId,
    VEICULO: visit.veiculoId
  };

  return (rules ?? [])
    .filter((rule) => rule?.ativo !== false)
    .filter((rule) => normalized(rule?.tipo) === 'BLOQUEIO')
    .filter((rule) => sameId(rule?.gateId, visit.gateId))
    .filter((rule) => {
      const scope = normalized(rule?.escopo);
      return sameId(rule?.referenciaId, referencesByScope[scope]);
    })
    .filter((rule) => {
      const start = timestamp(rule?.inicioVigencia);
      const end = timestamp(rule?.fimVigencia);
      return (start === null || now >= start) && (end === null || now <= end);
    })
    .map((rule) => `Regra de acesso do Gate: ${rule.motivo || 'bloqueio operacional ativo'}.`);
}

export function buildReceivingRows(visit, references = {}, complements = {}, options = {}) {
  if (!visit) return [];

  const bookings = references.bookings ?? [];
  const orders = references.ordens ?? [];
  const preadvices = references.preAvisos ?? [];
  const billsOfLading = complements.billsOfLading ?? references.billsOfLading ?? [];
  const now = timestamp(options.now) ?? Date.now();
  const visitAccessBlockers = accessRuleBlockers(visit, complements.regrasAcesso, now);

  return (visit.transacoes ?? []).map((transaction) => {
    const preadvice = findById(preadvices, transaction.preadviceId);
    const order = findById(orders, transaction.orderId ?? preadvice?.orderId);
    const booking = findById(bookings, transaction.bookingId ?? preadvice?.bookingId ?? order?.bookingId);
    const billOfLading = findById(billsOfLading, transaction.billOfLadingId ?? order?.billOfLadingId);
    const unit = transaction.unidadeReferencia || preadvice?.unidadeReferencia || order?.unidadeReferencia || '';
    const category = operationCategory(transaction.tipoOperacao, preadvice?.tipo);
    const iso = isoProfile(preadvice?.isoType);
    const blockers = [...visitAccessBlockers];
    const warnings = [];

    if (!unit.trim()) blockers.push('Transação sem unidade identificada.');
    if (transaction.troubleAtivo) blockers.push('Transação possui trouble ativo.');
    if (TERMINAL_TRANSACTION_STATUSES.has(normalized(transaction.status))) {
      blockers.push(`Transação em estado final ${transaction.status}.`);
    }
    blockers.push(...validateReferences(category, {
      transaction,
      booking,
      billOfLading,
      order,
      preadvice
    }, visit, now));

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
