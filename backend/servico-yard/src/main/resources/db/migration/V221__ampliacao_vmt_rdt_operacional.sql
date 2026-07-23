ALTER TABLE evento_vmt_work_instruction
    ADD COLUMN IF NOT EXISTS versao_contrato VARCHAR(20),
    ADD COLUMN IF NOT EXISTS numero_lacre VARCHAR(80),
    ADD COLUMN IF NOT EXISTS codigo_avaria VARCHAR(80),
    ADD COLUMN IF NOT EXISTS descricao_avaria VARCHAR(500),
    ADD COLUMN IF NOT EXISTS evidencia_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS reefer_conectado_desejado BOOLEAN,
    ADD COLUMN IF NOT EXISTS temperatura_reefer NUMERIC(8,3),
    ADD COLUMN IF NOT EXISTS unidade_alvo_rehandle VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rehandle_obrigatorio BOOLEAN,
    ADD COLUMN IF NOT EXISTS sequencia_rehandle INTEGER,
    ADD COLUMN IF NOT EXISTS etapa_anterior VARCHAR(80),
    ADD COLUMN IF NOT EXISTS etapa_nova VARCHAR(80),
    ADD COLUMN IF NOT EXISTS motivo_ajuste VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_evento_vmt_wi_tipo_ocorrido
    ON evento_vmt_work_instruction (ordem_trabalho_patio_id, tipo_evento, ocorrido_em);

CREATE INDEX IF NOT EXISTS idx_evento_vmt_wi_rehandle
    ON evento_vmt_work_instruction (ordem_trabalho_patio_id, unidade_alvo_rehandle, sequencia_rehandle);
