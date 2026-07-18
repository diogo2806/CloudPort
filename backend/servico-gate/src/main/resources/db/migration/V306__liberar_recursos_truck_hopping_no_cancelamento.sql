CREATE OR REPLACE FUNCTION liberar_recursos_truck_hopping_no_cancelamento()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CANCELADO' AND OLD.status IS DISTINCT FROM NEW.status THEN
        UPDATE gate_resource_occupation ocupacao
        SET ativo = FALSE,
            liberado_em = COALESCE(ocupacao.liberado_em, NOW()),
            updated_at = NOW()
        FROM gate_pass passagem
        WHERE passagem.agendamento_id = NEW.id
          AND ocupacao.gate_pass_id = passagem.id
          AND ocupacao.ativo = TRUE;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_liberar_recursos_truck_hopping_no_cancelamento ON agendamento;

CREATE TRIGGER trg_liberar_recursos_truck_hopping_no_cancelamento
    AFTER UPDATE OF status ON agendamento
    FOR EACH ROW
    EXECUTE FUNCTION liberar_recursos_truck_hopping_no_cancelamento();
