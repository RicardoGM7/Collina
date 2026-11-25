-- Módulo de espaços e reservas: restrições extras no banco (OPCIONAL, defesa em profundidade).
--
-- As tabelas tb_espaco_fisico, tb_bloco_horario e tb_reserva são criadas pelo Hibernate
-- (spring.jpa.hibernate.ddl-auto=update). A restrição única contra reserva duplicada
-- (ux_reserva_slot_ativo) já vem das entidades. Este script só acrescenta CHECKs que as
-- anotações JPA não expressam. Rode UMA VEZ depois da primeira subida da aplicação; é idempotente.
--
--   psql -h localhost -U <usuario> -d <banco> -f backend/sql/espacos_constraints.sql

DO $$
BEGIN
    -- RF-ESP01 / RD-04: espaço não público exige capacidade (AVCB) e o limite de convidados cabe nela
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_espaco_capacidade') THEN
        ALTER TABLE tb_espaco_fisico ADD CONSTRAINT ck_espaco_capacidade CHECK (
            limite_convidados >= 0
            AND (is_publico
                 OR (capacidade_maxima IS NOT NULL
                     AND capacidade_maxima > 0
                     AND limite_convidados <= capacidade_maxima))
        );
    END IF;

    -- RF-ESP02: bloco indivisível de 2 horas
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_bloco_duracao') THEN
        ALTER TABLE tb_bloco_horario ADD CONSTRAINT ck_bloco_duracao CHECK (
            hora_fim - hora_inicio = INTERVAL '2 hours'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_convidados') THEN
        ALTER TABLE tb_reserva ADD CONSTRAINT ck_reserva_convidados CHECK (qtd_convidados >= 0);
    END IF;

    -- Coerência entre status, cancelamento e a trava slot_ativo (ver Reserva.java):
    -- CONFIRMADA => slot_ativo = true e sem data de cancelamento; CANCELADA => slot_ativo NULL e com data
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_status_slot') THEN
        ALTER TABLE tb_reserva ADD CONSTRAINT ck_reserva_status_slot CHECK (
            (status = 'CONFIRMADA' AND slot_ativo IS TRUE AND cancelada_em IS NULL)
            OR (status = 'CANCELADA' AND slot_ativo IS NULL AND cancelada_em IS NOT NULL)
        );
    END IF;
END $$;
