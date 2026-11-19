ALTER TABLE compara_preco.categoria ADD COLUMN versao bigint NOT NULL DEFAULT 0;
ALTER TABLE compara_preco.marca ADD COLUMN versao bigint NOT NULL DEFAULT 0;
ALTER TABLE compara_preco.produto ADD COLUMN versao bigint NOT NULL DEFAULT 0;
ALTER TABLE compara_preco.variante_produto ADD COLUMN versao bigint NOT NULL DEFAULT 0;
CREATE TABLE compara_preco.configuracao_sistema (
    id integer PRIMARY KEY CHECK (id=1),
    abertura_contato varchar(160) NOT NULL CHECK (length(btrim(abertura_contato)) BETWEEN 3 AND 160),
    versao bigint NOT NULL DEFAULT 0
);
INSERT INTO compara_preco.configuracao_sistema(id, abertura_contato) VALUES (1, 'Olá! Encontrei sua oferta no Compara Preço e tenho interesse em');
