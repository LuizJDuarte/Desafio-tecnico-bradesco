-- Dados de exemplo para testar a replicação. Rode no banco origem depois do origem.sql:
--   psql -U postgres -d posto -f sql/origem_dados_exemplo.sql
-- Cadastros não duplicam se o script rodar de novo; cada execução registra um abastecimento novo.

SET client_encoding = 'UTF8';

INSERT INTO funcionarios (nome, cpf, cargo) VALUES
    ('Ana Souza',  '111.111.111-11', 'Frentista'),
    ('Bruno Lima', '222.222.222-22', 'Gerente')
ON CONFLICT (cpf) DO NOTHING;

INSERT INTO clientes (nome, documento, telefone) VALUES
    ('Carlos Pereira',               '333.333.333-33',     '(83) 99999-0001'),
    ('Transportadora Exemplo LTDA',  '12.345.678/0001-90', '(83) 3333-0002')
ON CONFLICT (documento) DO NOTHING;

INSERT INTO bombas (numero, combustivel, preco_litro) VALUES
    (1, 'Gasolina comum', 6.190),
    (2, 'Etanol',         4.290),
    (3, 'Diesel S10',     6.090)
ON CONFLICT (numero) DO NOTHING;

INSERT INTO abastecimentos (bomba_id, funcionario_id, cliente_id, litros, valor_total)
SELECT b.id, f.id, c.id, 40.000, ROUND(40.000 * b.preco_litro, 2)
FROM bombas b, funcionarios f, clientes c
WHERE b.numero = 1 AND f.cpf = '111.111.111-11' AND c.documento = '333.333.333-33';

INSERT INTO abastecimentos (bomba_id, funcionario_id, cliente_id, litros, valor_total)
SELECT b.id, f.id, NULL, 25.500, ROUND(25.500 * b.preco_litro, 2)
FROM bombas b, funcionarios f
WHERE b.numero = 3 AND f.cpf = '222.222.222-22';
