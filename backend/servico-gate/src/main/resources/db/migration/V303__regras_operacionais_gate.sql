DROP INDEX IF EXISTS uk_truck_visit_agendamento_ativo;

CREATE UNIQUE INDEX uk_truck_visit_agendamento
    ON truck_visit (agendamento_id)
    WHERE agendamento_id IS NOT NULL;

CREATE OR REPLACE FUNCTION gate_validar_truck_visit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_motivo VARCHAR(500);
    v_janela_id BIGINT;
BEGIN
    SELECT regra.motivo
      INTO v_motivo
      FROM gate_access_rule regra
     WHERE regra.gate_id = NEW.gate_id
       AND regra.ativo = TRUE
       AND regra.tipo = 'BLOQUEIO'
       AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
       AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
       AND (
           (regra.escopo = 'MOTORISTA' AND regra.referencia_id = NEW.motorista_id)
           OR (regra.escopo = 'TRANSPORTADORA' AND regra.referencia_id = NEW.transportadora_id)
           OR (regra.escopo = 'VEICULO' AND regra.referencia_id = NEW.veiculo_id)
       )
     ORDER BY regra.id
     LIMIT 1;

    IF v_motivo IS NOT NULL THEN
        RAISE EXCEPTION 'Acesso ao Gate bloqueado: %', v_motivo
            USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'MOTORISTA'
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) AND NOT EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'MOTORISTA'
           AND regra.referencia_id = NEW.motorista_id
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) THEN
        RAISE EXCEPTION 'Motorista sem permissão ativa para este Gate.'
            USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'TRANSPORTADORA'
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) AND NOT EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'TRANSPORTADORA'
           AND regra.referencia_id = NEW.transportadora_id
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) THEN
        RAISE EXCEPTION 'Transportadora sem permissão ativa para este Gate.'
            USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'VEICULO'
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) AND NOT EXISTS (
        SELECT 1
          FROM gate_access_rule regra
         WHERE regra.gate_id = NEW.gate_id
           AND regra.ativo = TRUE
           AND regra.tipo = 'PERMISSAO'
           AND regra.escopo = 'VEICULO'
           AND regra.referencia_id = NEW.veiculo_id
           AND (regra.inicio_vigencia IS NULL OR regra.inicio_vigencia <= NOW())
           AND (regra.fim_vigencia IS NULL OR regra.fim_vigencia >= NOW())
    ) THEN
        RAISE EXCEPTION 'Veículo sem permissão ativa para este Gate.'
            USING ERRCODE = 'P0001';
    END IF;

    IF NEW.agendamento_id IS NOT NULL THEN
        UPDATE janela_atendimento janela
           SET capacidade_utilizada = capacidade_utilizada + 1,
               updated_at = NOW()
          FROM agendamento agendamento
         WHERE agendamento.id = NEW.agendamento_id
           AND agendamento.janela_atendimento_id = janela.id
           AND janela.capacidade_utilizada < janela.capacidade
        RETURNING janela.id INTO v_janela_id;

        IF v_janela_id IS NULL THEN
            RAISE EXCEPTION 'A janela do agendamento não possui capacidade disponível.'
                USING ERRCODE = 'P0001';
        END IF;

        UPDATE agendamento
           SET facility_id = NEW.facility_id,
               gate_id = NEW.gate_id,
               status = 'EM_EXECUCAO',
               horario_real_chegada = COALESCE(horario_real_chegada, NOW()),
               updated_at = NOW()
         WHERE id = NEW.agendamento_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_gate_validar_truck_visit
BEFORE INSERT ON truck_visit
FOR EACH ROW
EXECUTE FUNCTION gate_validar_truck_visit();

CREATE OR REPLACE FUNCTION gate_resolver_bl_transacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_bl_id BIGINT;
    v_bl_disponivel BOOLEAN;
BEGIN
    IF NEW.order_id IS NOT NULL THEN
        SELECT ordem.bill_of_lading_id
          INTO v_bl_id
          FROM gate_order ordem
         WHERE ordem.id = NEW.order_id;

        NEW.bill_of_lading_id := COALESCE(NEW.bill_of_lading_id, v_bl_id);
    END IF;

    IF NEW.bill_of_lading_id IS NOT NULL THEN
        SELECT EXISTS (
            SELECT 1
              FROM gate_bill_of_lading bl
             WHERE bl.id = NEW.bill_of_lading_id
               AND bl.status IN ('ATIVO', 'PARCIAL')
               AND bl.quantidade_liberada < bl.quantidade_total
               AND (bl.validade_inicio IS NULL OR bl.validade_inicio <= NOW())
               AND (bl.validade_fim IS NULL OR bl.validade_fim >= NOW())
        ) INTO v_bl_disponivel;

        IF NOT v_bl_disponivel THEN
            RAISE EXCEPTION 'Bill of Lading indisponível, expirado ou sem saldo.'
                USING ERRCODE = 'P0001';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_gate_resolver_bl_transacao
BEFORE INSERT OR UPDATE OF order_id, bill_of_lading_id ON gate_transaction
FOR EACH ROW
EXECUTE FUNCTION gate_resolver_bl_transacao();

CREATE OR REPLACE FUNCTION gate_consumir_bl_transacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_atualizados INTEGER;
BEGIN
    IF NEW.status = 'CONCLUIDA'
       AND OLD.status <> 'CONCLUIDA'
       AND NEW.bill_of_lading_id IS NOT NULL THEN
        UPDATE gate_bill_of_lading
           SET quantidade_liberada = quantidade_liberada + 1,
               status = CASE
                   WHEN quantidade_liberada + 1 >= quantidade_total THEN 'LIBERADO'
                   ELSE 'PARCIAL'
               END,
               updated_at = NOW()
         WHERE id = NEW.bill_of_lading_id
           AND quantidade_liberada < quantidade_total
           AND status IN ('ATIVO', 'PARCIAL');

        GET DIAGNOSTICS v_atualizados = ROW_COUNT;
        IF v_atualizados = 0 THEN
            RAISE EXCEPTION 'Bill of Lading sem saldo para concluir a transação.'
                USING ERRCODE = 'P0001';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_gate_consumir_bl_transacao
AFTER UPDATE OF status ON gate_transaction
FOR EACH ROW
EXECUTE FUNCTION gate_consumir_bl_transacao();