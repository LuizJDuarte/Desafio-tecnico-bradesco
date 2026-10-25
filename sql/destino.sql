-- Banco Destino (clone da origem). Rode uma vez para cada destino:
--
-- 1) Crie o banco:            CREATE DATABASE posto_destino1;
-- 2) Rode este script nele:   psql -U postgres -d posto_destino1 -f sql/destino.sql
--
-- Mesma estrutura da origem, sem a replication_queue e sem triggers.
-- Os ids vêm da origem, por isso aqui são BIGINT simples, sem sequência.

SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS funcionarios (
    id     BIGINT       PRIMARY KEY,
    nome   VARCHAR(120) NOT NULL,
    cpf    VARCHAR(14)  NOT NULL UNIQUE,
    cargo  VARCHAR(60),
    ativo  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS clientes (
    id         BIGINT       PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    documento  VARCHAR(18)  UNIQUE,
    telefone   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS bombas (
    id           BIGINT        PRIMARY KEY,
    numero       INTEGER       NOT NULL UNIQUE,
    combustivel  VARCHAR(30)   NOT NULL,
    preco_litro  NUMERIC(10,3) NOT NULL CHECK (preco_litro > 0)
);

CREATE TABLE IF NOT EXISTS abastecimentos (
    id              BIGINT        PRIMARY KEY,
    bomba_id        BIGINT        NOT NULL REFERENCES bombas (id),
    funcionario_id  BIGINT        NOT NULL REFERENCES funcionarios (id),
    cliente_id      BIGINT        REFERENCES clientes (id),
    litros          NUMERIC(10,3) NOT NULL CHECK (litros > 0),
    valor_total     NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0),
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW()
);
