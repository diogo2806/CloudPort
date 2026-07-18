const HORA_MS = 60 * 60 * 1000;
const MINUTO_MS = 60 * 1000;

export const LINHAS_FERROVIARIAS = [
  'Recepção',
  'Linha 01',
  'Linha 02',
  'Linha 03',
  'Linha de apoio',
  'Expedição'
];

export const LINHAS_OPERACIONAIS = ['Linha 01', 'Linha 02', 'Linha 03'];

export function converterDataFerroviaria(valor) {
  if (!valor) return null;
  const data = valor instanceof Date ? new Date(valor.getTime()) : new Date(valor);
  return Number.isNaN(data.getTime()) ? null : data;
}

function normalizarLista(valor) {
  return Array.isArray(valor) ? valor : [];
}

export function normalizarVisitasFerrovia(payload) {
  if (!Array.isArray(payload)) return [];
  return payload.map((visita, indice) => {
    const chegada = converterDataFerroviaria(visita?.horaChegadaPrevista);
    if (!chegada) return null;
    let partida = converterDataFerroviaria(visita?.horaPartidaPrevista);
    if (!partida || partida.getTime() <= chegada.getTime()) {
      partida = new Date(chegada.getTime() + 12 * HORA_MS);
    }
    const listaVagoes = normalizarLista(visita?.listaVagoes);
    const listaCarga = normalizarLista(visita?.listaCarga);
    const listaDescarga = normalizarLista(visita?.listaDescarga);
    return {
      ...visita,
      chave: String(visita?.id ?? `${visita?.identificadorTrem ?? 'trem'}-${indice}`),
      identificadorTrem: visita?.identificadorTrem || `Trem ${indice + 1}`,
      operadoraFerroviaria: visita?.operadoraFerroviaria || 'Operadora não informada',
      statusVisita: visita?.statusVisita || 'PLANEJADO',
      tipoVisita: visita?.tipoVisita || 'COMPOSICAO_FERROVIARIA',
      chegada,
      partida,
      quantidadeVagoes: listaVagoes.length,
      quantidadeCarga: listaCarga.length,
      quantidadeDescarga: listaDescarga.length
    };
  }).filter(Boolean).sort((a, b) => a.chegada.getTime() - b.chegada.getTime());
}

function limitarDuracaoEtapa(duracaoTotal) {
  return Math.min(2 * HORA_MS, Math.max(30 * MINUTO_MS, duracaoTotal * 0.15));
}

function descricaoOperacao(visita) {
  if (visita.tipoVisita === 'LOCOMOTIVA_ISOLADA') return 'Apoio de locomotiva';
  if (visita.quantidadeCarga && visita.quantidadeDescarga) return 'Carga e descarga';
  if (visita.quantidadeDescarga) return 'Descarga';
  if (visita.quantidadeCarga) return 'Carga';
  return 'Operação ferroviária';
}

function linhaOperacional(visita, indice, linhasOperacionais) {
  if (visita.tipoVisita === 'LOCOMOTIVA_ISOLADA') return 'Linha de apoio';
  return linhasOperacionais[indice % linhasOperacionais.length];
}

function criarSegmentos(visita, indice, linhasOperacionais) {
  const inicio = visita.chegada.getTime();
  const fim = visita.partida.getTime();
  const duracao = fim - inicio;
  const duracaoExtremidade = limitarDuracaoEtapa(duracao);
  let fimRecepcao = inicio + duracaoExtremidade;
  let inicioExpedicao = fim - duracaoExtremidade;
  if (inicioExpedicao <= fimRecepcao) {
    fimRecepcao = inicio + duracao * 0.25;
    inicioExpedicao = inicio + duracao * 0.75;
  }
  const linhaOperacao = linhaOperacional(visita, indice, linhasOperacionais);
  return [
    {
      chave: `${visita.chave}-recepcao`,
      visitaChave: visita.chave,
      visita,
      etapa: 'RECEPCAO',
      descricao: 'Recepção e conferência',
      linha: 'Recepção',
      inicio: new Date(inicio),
      fim: new Date(fimRecepcao)
    },
    {
      chave: `${visita.chave}-operacao`,
      visitaChave: visita.chave,
      visita,
      etapa: 'OPERACAO',
      descricao: descricaoOperacao(visita),
      linha: linhaOperacao,
      inicio: new Date(fimRecepcao),
      fim: new Date(inicioExpedicao)
    },
    {
      chave: `${visita.chave}-expedicao`,
      visitaChave: visita.chave,
      visita,
      etapa: 'EXPEDICAO',
      descricao: 'Formação e expedição',
      linha: 'Expedição',
      inicio: new Date(inicioExpedicao),
      fim: new Date(fim)
    }
  ];
}

function sobrepoe(primeiro, segundo) {
  return primeiro.inicio.getTime() < segundo.fim.getTime()
    && segundo.inicio.getTime() < primeiro.fim.getTime();
}

function distribuirFaixas(segmentos) {
  const porLinha = new Map();
  segmentos.forEach((segmento) => {
    if (!porLinha.has(segmento.linha)) porLinha.set(segmento.linha, []);
    porLinha.get(segmento.linha).push(segmento);
  });

  porLinha.forEach((itens) => {
    itens.sort((a, b) => a.inicio.getTime() - b.inicio.getTime());
    const fimPorFaixa = [];
    itens.forEach((segmento) => {
      let faixa = fimPorFaixa.findIndex((fim) => fim <= segmento.inicio.getTime());
      if (faixa < 0) faixa = fimPorFaixa.length;
      fimPorFaixa[faixa] = segmento.fim.getTime();
      segmento.faixa = faixa;
    });
    itens.forEach((segmento) => {
      segmento.totalFaixas = Math.max(1, fimPorFaixa.length);
    });
  });
}

function marcarConflitos(segmentos) {
  const porLinha = new Map();
  segmentos.forEach((segmento) => {
    if (!porLinha.has(segmento.linha)) porLinha.set(segmento.linha, []);
    porLinha.get(segmento.linha).push(segmento);
  });
  porLinha.forEach((itens) => {
    for (let atual = 0; atual < itens.length; atual += 1) {
      for (let proximo = atual + 1; proximo < itens.length; proximo += 1) {
        if (!sobrepoe(itens[atual], itens[proximo])) continue;
        itens[atual].conflito = true;
        itens[proximo].conflito = true;
      }
    }
  });
}

export function construirOcupacoesFerroviarias(visitas, opcoes = {}) {
  const linhasOperacionais = Array.isArray(opcoes.linhasOperacionais) && opcoes.linhasOperacionais.length
    ? opcoes.linhasOperacionais
    : LINHAS_OPERACIONAIS;
  const segmentos = visitas.flatMap((visita, indice) => criarSegmentos(visita, indice, linhasOperacionais));
  marcarConflitos(segmentos);
  distribuirFaixas(segmentos);
  return segmentos;
}

export function calcularJanelaFerroviaria(visitas) {
  if (!visitas.length) {
    const agora = new Date();
    return { inicio: agora, fim: new Date(agora.getTime() + 24 * HORA_MS) };
  }
  const inicio = new Date(Math.min(...visitas.map((visita) => visita.chegada.getTime())) - HORA_MS);
  let fim = new Date(Math.max(...visitas.map((visita) => visita.partida.getTime())) + HORA_MS);
  if (fim.getTime() - inicio.getTime() < 24 * HORA_MS) {
    fim = new Date(inicio.getTime() + 24 * HORA_MS);
  }
  return { inicio, fim };
}

export function gerarMarcadoresFerroviarios(janela, quantidade = 9) {
  const total = Math.max(janela.fim.getTime() - janela.inicio.getTime(), 1);
  return Array.from({ length: quantidade }, (_, indice) => {
    const percentual = quantidade === 1 ? 0 : indice / (quantidade - 1);
    return {
      percentual: percentual * 100,
      data: new Date(janela.inicio.getTime() + total * percentual)
    };
  });
}

export function calcularPosicaoVertical(segmento, janela) {
  const total = Math.max(janela.fim.getTime() - janela.inicio.getTime(), 1);
  const inicio = Math.max(segmento.inicio.getTime(), janela.inicio.getTime());
  const fim = Math.min(segmento.fim.getTime(), janela.fim.getTime());
  const top = Math.max(0, Math.min(100, ((inicio - janela.inicio.getTime()) / total) * 100));
  const height = Math.max(1, Math.min(100 - top, ((Math.max(fim - inicio, 1)) / total) * 100));
  return { top: `${top}%`, height: `${height}%` };
}

export function calcularPercentualInstante(instante, janela) {
  const data = converterDataFerroviaria(instante);
  if (!data) return 0;
  const total = Math.max(janela.fim.getTime() - janela.inicio.getTime(), 1);
  return Math.max(0, Math.min(100, ((data.getTime() - janela.inicio.getTime()) / total) * 100));
}

export function calcularFaseTrem(visita, segmentos, instante) {
  const data = converterDataFerroviaria(instante);
  if (!data) return 'INDEFINIDA';
  const tempo = data.getTime();
  if (tempo < visita.chegada.getTime()) return 'PREVISTO';
  if (tempo >= visita.partida.getTime()) return 'PARTIU';
  const atual = segmentos.find((segmento) => segmento.visitaChave === visita.chave
    && tempo >= segmento.inicio.getTime()
    && tempo < segmento.fim.getTime());
  if (atual?.etapa === 'RECEPCAO') return 'RECEBENDO';
  if (atual?.etapa === 'OPERACAO') return visita.tipoVisita === 'LOCOMOTIVA_ISOLADA' ? 'EM_APOIO' : 'OPERANDO';
  if (atual?.etapa === 'EXPEDICAO') return 'EXPEDINDO';
  return 'NO_TERMINAL';
}

export function construirSimulacaoFerroviaria(visitas, segmentos, instante) {
  const data = converterDataFerroviaria(instante) ?? new Date();
  const tempo = data.getTime();
  return visitas.map((visita) => {
    const segmentosVisita = segmentos.filter((segmento) => segmento.visitaChave === visita.chave);
    const segmentoAtual = segmentosVisita.find((segmento) => tempo >= segmento.inicio.getTime() && tempo < segmento.fim.getTime());
    const duracao = Math.max(visita.partida.getTime() - visita.chegada.getTime(), 1);
    const progresso = Math.max(0, Math.min(100, Math.round(((tempo - visita.chegada.getTime()) / duracao) * 100)));
    return {
      ...visita,
      faseSimulada: calcularFaseTrem(visita, segmentosVisita, data),
      linhaAtual: segmentoAtual?.linha ?? (tempo < visita.chegada.getTime() ? 'Fora do terminal' : tempo >= visita.partida.getTime() ? 'Expedido' : 'Em manobra'),
      progressoOperacao: progresso,
      conflitoLinha: segmentosVisita.some((segmento) => segmento.conflito)
    };
  });
}

export function paraDateTimeLocalFerroviario(valor) {
  const data = converterDataFerroviaria(valor);
  if (!data) return '';
  const local = new Date(data.getTime() - data.getTimezoneOffset() * MINUTO_MS);
  return local.toISOString().slice(0, 16);
}

export function somarHorasFerroviarias(valor, horas) {
  const data = converterDataFerroviaria(valor) ?? new Date();
  return new Date(data.getTime() + Number(horas || 0) * HORA_MS);
}
