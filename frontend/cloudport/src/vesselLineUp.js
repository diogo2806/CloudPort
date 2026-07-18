const HORA_MS = 60 * 60 * 1000;
const DURACAO_PADRAO_OPERACAO_HORAS = 12;

export function converterData(valor) {
  if (!valor) return null;
  const data = valor instanceof Date ? new Date(valor.getTime()) : new Date(valor);
  return Number.isNaN(data.getTime()) ? null : data;
}

function primeiraDataValida(...valores) {
  return valores.map(converterData).find(Boolean) ?? null;
}

export function normalizarEscalasLineUp(payload) {
  if (!Array.isArray(payload)) return [];
  return payload.map((escala, indice) => {
    const eta = primeiraDataValida(escala.chegadaEfetiva, escala.chegadaPrevista);
    if (!eta) return null;
    const etb = primeiraDataValida(escala.atracacaoEfetiva, escala.atracacaoPrevista, eta) ?? eta;
    let etd = primeiraDataValida(escala.partidaEfetiva, escala.partidaPrevista);
    if (!etd || etd.getTime() <= etb.getTime()) {
      etd = new Date(etb.getTime() + DURACAO_PADRAO_OPERACAO_HORAS * HORA_MS);
    }
    return {
      ...escala,
      chave: String(escala.id ?? `${escala.codigoImo ?? 'navio'}-${indice}`),
      eta,
      etb,
      etd,
      berco: escala.bercoAtual || escala.bercoPrevisto || 'Sem berço',
      nomeNavio: escala.nomeNavio || escala.codigoImo || 'Navio sem identificação'
    };
  }).filter(Boolean).sort((a, b) => a.eta.getTime() - b.eta.getTime());
}

export function calcularFaseSimulada(escala, instante) {
  const simulacao = converterData(instante);
  if (!simulacao || !escala?.eta || !escala?.etb || !escala?.etd) return 'INDEFINIDA';
  const tempo = simulacao.getTime();
  if (tempo < escala.eta.getTime()) return 'PREVISTA';
  if (tempo < escala.etb.getTime()) return 'INBOUND';
  if (tempo < escala.etd.getTime()) return 'OPERANDO';
  return 'PARTIU';
}

export function calcularConflitosBerco(escalas) {
  const conflitos = new Set();
  const porBerco = new Map();
  escalas.forEach((escala) => {
    if (!escala?.berco || escala.berco === 'Sem berço') return;
    if (!porBerco.has(escala.berco)) porBerco.set(escala.berco, []);
    porBerco.get(escala.berco).push(escala);
  });

  porBerco.forEach((itens) => {
    itens.sort((a, b) => a.etb.getTime() - b.etb.getTime());
    for (let atual = 0; atual < itens.length; atual += 1) {
      for (let proximo = atual + 1; proximo < itens.length; proximo += 1) {
        if (itens[proximo].etb.getTime() >= itens[atual].etd.getTime()) break;
        conflitos.add(itens[atual].chave);
        conflitos.add(itens[proximo].chave);
      }
    }
  });
  return conflitos;
}

function calcularProgresso(escala, instante) {
  const tempo = instante.getTime();
  const inicio = escala.etb.getTime();
  const fim = escala.etd.getTime();
  if (tempo <= inicio) return 0;
  if (tempo >= fim) return 100;
  return Math.round(((tempo - inicio) / (fim - inicio)) * 100);
}

function calcularAtrasoChegada(escala) {
  const prevista = converterData(escala.chegadaPrevista);
  const efetiva = converterData(escala.chegadaEfetiva);
  if (!prevista || !efetiva) return null;
  return Math.round((efetiva.getTime() - prevista.getTime()) / (60 * 1000));
}

export function construirSimulacao(escalas, instante) {
  const simulacao = converterData(instante) ?? new Date();
  const conflitos = calcularConflitosBerco(escalas);
  return escalas.map((escala) => ({
    ...escala,
    faseSimulada: calcularFaseSimulada(escala, simulacao),
    conflitoBerco: conflitos.has(escala.chave),
    progressoOperacao: calcularProgresso(escala, simulacao),
    atrasoChegadaMinutos: calcularAtrasoChegada(escala)
  }));
}

export function calcularJanelaTimeline(escalas) {
  if (!escalas.length) {
    const agora = new Date();
    return { inicio: agora, fim: new Date(agora.getTime() + 24 * HORA_MS) };
  }
  const menor = Math.min(...escalas.map((escala) => escala.eta.getTime()));
  const maior = Math.max(...escalas.map((escala) => escala.etd.getTime()));
  const inicio = new Date(menor - 3 * HORA_MS);
  let fim = new Date(maior + 3 * HORA_MS);
  if (fim.getTime() - inicio.getTime() < 24 * HORA_MS) {
    fim = new Date(inicio.getTime() + 24 * HORA_MS);
  }
  return { inicio, fim };
}

function calcularIntervaloPercentual(escala, janela) {
  const total = Math.max(janela.fim.getTime() - janela.inicio.getTime(), 1);
  const inicio = Math.max(escala.etb.getTime(), janela.inicio.getTime());
  const fim = Math.min(escala.etd.getTime(), janela.fim.getTime());
  const deslocamento = Math.max(0, Math.min(100, ((inicio - janela.inicio.getTime()) / total) * 100));
  const tamanho = Math.max(1.5, Math.min(100 - deslocamento, ((Math.max(fim - inicio, 1)) / total) * 100));
  return { deslocamento, tamanho };
}

export function calcularPosicaoTimeline(escala, janela) {
  const intervalo = calcularIntervaloPercentual(escala, janela);
  return { left: `${intervalo.deslocamento}%`, width: `${intervalo.tamanho}%` };
}

export function calcularPosicaoVerticalTimeline(escala, janela) {
  const intervalo = calcularIntervaloPercentual(escala, janela);
  return { top: `${intervalo.deslocamento}%`, height: `${intervalo.tamanho}%` };
}

export function calcularAlturaTimelineVertical(janela) {
  const duracaoHoras = Math.max(24, (janela.fim.getTime() - janela.inicio.getTime()) / HORA_MS);
  return Math.round(Math.max(680, Math.min(1800, duracaoHoras * 2.5)));
}

export function gerarMarcadoresTimeline(janela, quantidade = 8) {
  const total = janela.fim.getTime() - janela.inicio.getTime();
  return Array.from({ length: quantidade }, (_, indice) => {
    const percentual = quantidade === 1 ? 0 : indice / (quantidade - 1);
    return {
      percentual: percentual * 100,
      data: new Date(janela.inicio.getTime() + total * percentual)
    };
  });
}

function distribuirFaixas(itens) {
  const fimPorFaixa = [];
  const distribuidos = [...itens]
    .sort((a, b) => a.etb.getTime() - b.etb.getTime())
    .map((escala) => {
      let faixa = fimPorFaixa.findIndex((fim) => fim <= escala.etb.getTime());
      if (faixa < 0) {
        faixa = fimPorFaixa.length;
        fimPorFaixa.push(escala.etd.getTime());
      } else {
        fimPorFaixa[faixa] = escala.etd.getTime();
      }
      return { ...escala, faixa };
    });
  return { itens: distribuidos, totalFaixas: Math.max(fimPorFaixa.length, 1) };
}

export function agruparEscalasPorBerco(escalas) {
  const grupos = new Map();
  escalas.forEach((escala) => {
    if (!grupos.has(escala.berco)) grupos.set(escala.berco, []);
    grupos.get(escala.berco).push(escala);
  });
  return Array.from(grupos, ([berco, itens]) => ({ berco, ...distribuirFaixas(itens) }))
    .sort((a, b) => a.berco.localeCompare(b.berco, 'pt-BR'));
}

export function paraDateTimeLocal(valor) {
  const data = converterData(valor);
  if (!data) return '';
  const local = new Date(data.getTime() - data.getTimezoneOffset() * 60 * 1000);
  return local.toISOString().slice(0, 16);
}

export function somarHoras(valor, horas) {
  const data = converterData(valor) ?? new Date();
  return new Date(data.getTime() + Number(horas || 0) * HORA_MS);
}
