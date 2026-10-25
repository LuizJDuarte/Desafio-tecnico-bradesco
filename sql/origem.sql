-- Banco Origem (sistema do posto de combustível)
--
-- 1) Crie o banco:            CREATE DATABASE posto;
-- 2) Rode este script nele:   psql -U postgres -d posto -f sql/origem.sql
--
-- Toda tabela replicada precisa de chave primária "id" BIGINT: é por ela que o replicador
-- encontra a linha no destino quando aplica os eventos da replication_queue.

SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS funcionarios (
    id     BIGSERIAL    PRIMARY KEY,
    nome   VARCHAR(120) NOT NULL,
    cpf    VARCHAR(14)  NOT NULL UNIQUE,
    cargo  VARCHAR(60),
    ativo  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS clientes (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    documento  VARCHAR(18)  UNIQUE,
    telefone   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS bombas (
    id           BIGSERIAL     PRIMARY KEY,
    numero       INTEGER       NOT NULL UNIQUE,
    combustivel  VARCHAR(30)   NOT NULL,
    preco_litro  NUMERIC(10,3) NOT NULL CHECK (preco_litro > 0)
);

CREATE TABLE IF NOT EXISTS abastecimentos (
    id              BIGSERIAL     PRIMARY KEY,
    bomba_id        BIGINT        NOT NULL REFERENCES bombas (id),
    funcionario_id  BIGINT        NOT NULL REFERENCES funcionarios (id),
    cliente_id      BIGINT        REFERENCES clientes (id),
    litros          NUMERIC(10,3) NOT NULL CHECK (litros > 0),
    valor_total     NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0),
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Fila de alterações, preenchida pelos triggers abaixo.
-- O replicador só lê esta tabela; o ponto até onde cada destino já aplicou fica no
-- banco de controle (TB_REPLICACAO_CHECKPOINT). Por isso o usuário da origem pode ser somente leitura.
CREATE TABLE IF NOT EXISTS replication_queue (
    id           BIGSERIAL    PRIMARY KEY,
    table_name   VARCHAR(127) NOT NULL,
    row_id       BIGINT       NOT NULL,
    operation    CHAR(1)      NOT NULL CHECK (operation IN ('I', 'U', 'D')),
    occurred_at  TIMESTAMP    NOT NULL DEFAULT clock_timestamp()
);

-- Registra I (insert), U (update) e D (delete) com o id da linha alterada.
-- Se um update trocar o id, o id antigo é registrado como D para não sobrar linha velha no destino.
CREATE OR REPLACE FUNCTION fn_registrar_alteracao() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO replication_queue (table_name, row_id, operation) VALUES (TG_TABLE_NAME, OLD.id, 'D');
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.id <> OLD.id THEN
        INSERT INTO replication_queue (table_name, row_id, operation) VALUES (TG_TABLE_NAME, OLD.id, 'D');
    END IF;

    INSERT INTO replication_queue (table_name, row_id, operation) VALUES (TG_TABLE_NAME, NEW.id, LEFT(TG_OP, 1));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_replicacao ON funcionarios;
CREATE TRIGGER trg_replicacao AFTER INSERT OR UPDATE OR DELETE ON funcionarios
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_alteracao();

DROP TRIGGER IF EXISTS trg_replicacao ON clientes;
CREATE TRIGGER trg_replicacao AFTER INSERT OR UPDATE OR DELETE ON clientes
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_alteracao();

DROP TRIGGER IF EXISTS trg_replicacao ON bombas;
CREATE TRIGGER trg_replicacao AFTER INSERT OR UPDATE OR DELETE ON bombas
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_alteracao();

DROP TRIGGER IF EXISTS trg_replicacao ON abastecimentos;
CREATE TRIGGER trg_replicacao AFTER INSERT OR UPDATE OR DELETE ON abastecimentos
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_alteracao();
