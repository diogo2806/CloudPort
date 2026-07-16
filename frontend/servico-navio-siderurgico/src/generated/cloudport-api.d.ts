/*
 * Arquivo gerado pelo comando npm run generate:api-types.
 * Fonte: OpenAPI consolidado do cloudport-monolito-navio em /api-docs.
 */

export interface paths {
  "/api/public/v1/vessel-visits": {
    get: operations["listarVisitas"];
  };
  "/api/public/v1/vessel-visits/{id}": {
    get: operations["detalharVisita"];
  };
  "/api/public/v1/events/stream": {
    get: operations["assinarEventosPublicos"];
  };
}

export interface components {
  schemas: {
    ErroApi: {
      codigo?: string;
      mensagem?: string;
      detalhes?: Record<string, unknown>;
      correlationId?: string;
      timestamp?: string;
    };
    VisitaNavioResumoDTO: {
      id?: number;
      navioId?: number;
      navioNome?: string;
      codigoVisita?: string;
      viagemEntrada?: string;
      viagemSaida?: string;
      linhaOperadora?: string;
      bercoPrevisto?: string;
      bercoAtual?: string;
      eta?: string;
      etb?: string;
      etd?: string;
      fase?: "PREVISTA" | "FUNDEADA" | "ATRACADA" | "OPERANDO" | "OPERACAO_CONCLUIDA" | "PARTIU" | "CANCELADA";
      atualizadoEm?: string;
    };
    PaginaRespostaVisitaNavioResumoDTO: {
      conteudo?: components["schemas"]["VisitaNavioResumoDTO"][];
      pagina?: number;
      tamanho?: number;
      totalElementos?: number;
      totalPaginas?: number;
      primeira?: boolean;
      ultima?: boolean;
    };
    EventoIntegracaoV1: {
      eventId?: string;
      eventType?: string;
      eventVersion?: number;
      occurredAt?: string;
      correlationId?: string;
      source?: string;
      data?: unknown;
    };
  };
}

export interface operations {
  listarVisitas: {
    parameters: {
      query?: {
        fase?: components["schemas"]["VisitaNavioResumoDTO"]["fase"];
        dataInicio?: string;
        dataFim?: string;
        navioId?: number;
        codigoVisita?: string;
        berco?: string;
        linhaOperadora?: string;
        pagina?: number;
        tamanho?: number;
        ordenarPor?: string;
        direcao?: "ASC" | "DESC";
        campos?: string;
      };
      header?: {
        "X-CloudPort-Client-Id"?: string;
        "X-CloudPort-Client-Secret"?: string;
        "X-Correlation-Id"?: string;
      };
    };
    responses: {
      200: { content: { "application/json": components["schemas"]["PaginaRespostaVisitaNavioResumoDTO"] } };
      400: { content: { "application/json": components["schemas"]["ErroApi"] } };
      401: { content: { "application/json": components["schemas"]["ErroApi"] } };
    };
  };
  detalharVisita: {
    parameters: {
      path: { id: number };
      query?: { campos?: string };
    };
    responses: {
      200: { content: { "application/json": Record<string, unknown> } };
      404: { content: { "application/json": components["schemas"]["ErroApi"] } };
    };
  };
  assinarEventosPublicos: {
    parameters: {
      query?: { visitaId?: number };
    };
    responses: {
      200: { content: { "text/event-stream": components["schemas"]["EventoIntegracaoV1"] } };
    };
  };
}
