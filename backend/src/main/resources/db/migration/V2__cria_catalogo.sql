CREATE FUNCTION compara_preco.normalizar_texto(valor text)
RETURNS text LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT lower(btrim(regexp_replace(valor, '[[:space:]]+', ' ', 'g')));
$$;

CREATE TABLE compara_preco.categoria (
    id uuid PRIMARY KEY,
    nome varchar(100) NOT NULL CHECK (compara_preco.normalizar_texto(nome) <> ''),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX uk_categoria_nome ON compara_preco.categoria (compara_preco.normalizar_texto(nome));

CREATE TABLE compara_preco.marca (
    id uuid PRIMARY KEY,
    nome varchar(100) NOT NULL CHECK (compara_preco.normalizar_texto(nome) <> ''),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX uk_marca_nome ON compara_preco.marca (compara_preco.normalizar_texto(nome));

CREATE TABLE compara_preco.produto (
    id uuid PRIMARY KEY,
    categoria_id uuid NOT NULL REFERENCES compara_preco.categoria(id),
    marca_id uuid NOT NULL REFERENCES compara_preco.marca(id),
    nome varchar(160) NOT NULL CHECK (compara_preco.normalizar_texto(nome) <> ''),
    modelo varchar(120) NOT NULL CHECK (compara_preco.normalizar_texto(modelo) <> ''),
    descricao varchar(2000),
    imagem_url varchar(2000) CHECK (imagem_url IS NULL OR imagem_url ~ '^https?://[^/[:space:]]+'),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX uk_produto_marca_modelo ON compara_preco.produto (marca_id, compara_preco.normalizar_texto(modelo));
CREATE INDEX idx_produto_categoria ON compara_preco.produto (categoria_id);

-- A forma canônica também é exigida no banco para impedir duplicatas por SQL/importações.
CREATE FUNCTION compara_preco.atributos_variante_validos(atributos jsonb)
RETURNS boolean LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT CASE WHEN jsonb_typeof(atributos) <> 'object' THEN false ELSE (
        SELECT count(*) <= 8 AND coalesce(bool_and(
            chave ~ '^[a-z][a-z0-9_]{0,31}$'
            AND chave NOT IN ('cor', 'condicao', 'armazenamento_gb')
            AND jsonb_typeof(valor) = 'string'
            AND length(valor #>> '{}') BETWEEN 1 AND 64
            AND (valor #>> '{}') = compara_preco.normalizar_texto(valor #>> '{}')
            AND (valor #>> '{}') !~ '[[:cntrl:]]'
        ), true)
        FROM jsonb_each(atributos) AS par(chave, valor)
    ) END;
$$;

CREATE TABLE compara_preco.variante_produto (
    id uuid PRIMARY KEY,
    produto_id uuid NOT NULL REFERENCES compara_preco.produto(id),
    armazenamento_gb integer CHECK (armazenamento_gb IS NULL OR armazenamento_gb > 0),
    cor varchar(80) CHECK (cor IS NULL OR (
        length(cor) > 0 AND cor = compara_preco.normalizar_texto(cor) AND cor !~ '[[:cntrl:]]'
    )),
    condicao varchar(20) NOT NULL CHECK (condicao IN ('NOVO', 'USADO', 'RECONDICIONADO')),
    atributos jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (
        compara_preco.atributos_variante_validos(atributos) AND octet_length(atributos::text) <= 1400
    ),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    CONSTRAINT uk_variante_equivalente UNIQUE NULLS NOT DISTINCT
        (produto_id, armazenamento_gb, cor, condicao, atributos)
);
