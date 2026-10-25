-- Banco de Controle (Gerencial)
--
-- 1) Crie o banco:            CREATE DATABASE controle;
-- 2) Rode este script nele:   psql -U postgres -d controle -f sql/controle.sql
--
-- O script pode ser executado mais de uma vez: tabelas e dados de exemplo só são criados se não existirem.

SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS TB_REPLICACAO_PROCESSO (
    ID          BIGSERIAL    PRIMARY KEY,
    PROCESSO    VARCHAR(100) NOT NULL,
    DESCRICAO   VARCHAR(255),
    HABILITADO  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tabelas replicadas em cada processo.
-- ORDEM define a sequência da carga: tabelas referenciadas por chave estrangeira vêm antes.
-- DS_WHERE é opcional (vazio = tabela inteira) e não aceita ';' nem comentários.
CREATE TABLE IF NOT EXISTS TB_REPLICACAO_PROCESSO_TABELA (
    ID              BIGSERIAL    PRIMARY KEY,
    PROCESSO_ID     BIGINT       NOT NULL REFERENCES TB_REPLICACAO_PROCESSO (ID),
    TABELA_ORIGEM   VARCHAR(127) NOT NULL,
    TABELA_DESTINO  VARCHAR(127) NOT NULL,
    ORDEM           INTEGER      NOT NULL,
    HABILITADO      BOOLEAN      NOT NULL DEFAULT TRUE,
    DS_WHERE        TEXT
);

-- Conexões de origem e destino de cada processo.
-- As senhas são gravadas criptografadas pela aplicação (AES-256-GCM, chave em REPLICADOR_CHAVE).
CREATE TABLE IF NOT EXISTS TB_REPLICACAO_DIRECAO (
    ID               BIGSERIAL    PRIMARY KEY,
    PROCESSO_ID      BIGINT       NOT NULL REFERENCES TB_REPLICACAO_PROCESSO (ID),
    DIRECAO_ORIGEM   VARCHAR(500) NOT NULL,
    DIRECAO_DESTINO  VARCHAR(500) NOT NULL,
    USUARIO_ORIGEM   VARCHAR(100),
    USUARIO_DESTINO  VARCHAR(100),
    SENHA_ORIGEM     TEXT,
    SENHA_DESTINO    TEXT,
    HABILITADO       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Último evento da replication_queue (banco origem) que cada direção já aplicou no seu destino.
-- Fica aqui, e não na origem, para que cada destino avance no seu ritmo.
CREATE TABLE IF NOT EXISTS TB_REPLICACAO_CHECKPOINT (
    DIRECAO_ID      BIGINT    PRIMARY KEY REFERENCES TB_REPLICACAO_DIRECAO (ID) ON DELETE CASCADE,
    ULTIMO_ID_FILA  BIGINT    NOT NULL DEFAULT 0,
    ATUALIZADO_EM   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Processo de exemplo com as tabelas do posto, na ordem que respeita as chaves estrangeiras.
-- As direções (conexões) são cadastradas pela tela, para que as senhas sejam criptografadas.
INSERT INTO TB_REPLICACAO_PROCESSO (PROCESSO, DESCRICAO, HABILITADO)
SELECT 'POSTO', 'Replicação do banco do posto de combustível', TRUE
WHERE NOT EXISTS (SELECT 1 FROM TB_REPLICACAO_PROCESSO WHERE PROCESSO = 'POSTO');

INSERT INTO TB_REPLICACAO_PROCESSO_TABELA (PROCESSO_ID, TABELA_ORIGEM, TABELA_DESTINO, ORDEM, HABILITADO, DS_WHERE)
SELECT p.ID, t.tabela, t.tabela, t.ordem, TRUE, NULL
FROM TB_REPLICACAO_PROCESSO p
CROSS JOIN (VALUES ('funcionarios', 1), ('clientes', 2), ('bombas', 3), ('abastecimentos', 4)) AS t (tabela, ordem)
WHERE p.PROCESSO = 'POSTO'
  AND NOT EXISTS (
      SELECT 1 FROM TB_REPLICACAO_PROCESSO_TABELA x
      WHERE x.PROCESSO_ID = p.ID AND x.TABELA_ORIGEM = t.tabela
  );
