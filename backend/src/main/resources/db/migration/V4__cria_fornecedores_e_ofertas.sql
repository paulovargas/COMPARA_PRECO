CREATE FUNCTION compara_preco.atributos_obrigatorios_validos(atributos jsonb)
RETURNS boolean LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT CASE WHEN jsonb_typeof(atributos) <> 'array' THEN false ELSE (
        SELECT count(*) <= 8 AND count(*) = count(DISTINCT valor)
            AND coalesce(bool_and(jsonb_typeof(valor) = 'string'
                AND (valor #>> '{}') ~ '^[a-z][a-z0-9_]{0,31}$'
                AND (valor #>> '{}') NOT IN ('cor', 'condicao', 'armazenamento_gb')), true)
        FROM jsonb_array_elements(atributos) AS elemento(valor)
    ) END;
$$;

ALTER TABLE compara_preco.categoria
    ADD COLUMN exige_armazenamento boolean NOT NULL DEFAULT false,
    ADD COLUMN exige_cor boolean NOT NULL DEFAULT false,
    ADD COLUMN atributos_obrigatorios jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (compara_preco.atributos_obrigatorios_validos(atributos_obrigatorios));

-- Completa as regras das categorias demonstrativas já existentes, preservando seus dados de apresentação.
UPDATE compara_preco.categoria SET exige_armazenamento = true, exige_cor = true,
    atributos_obrigatorios = '["ram_gb"]'::jsonb
    WHERE id = '10000000-0000-0000-0000-000000000001';
UPDATE compara_preco.categoria SET exige_armazenamento = true, exige_cor = true,
    atributos_obrigatorios = '["ram_gb","tela_polegadas"]'::jsonb
    WHERE id = '10000000-0000-0000-0000-000000000002';

CREATE TABLE compara_preco.fornecedor (
    id uuid PRIMARY KEY,
    usuario_id uuid NOT NULL UNIQUE REFERENCES compara_preco.usuario(id),
    razao_social varchar(160) NOT NULL CHECK (length(btrim(razao_social)) >= 3),
    nome_fantasia varchar(120) NOT NULL CHECK (length(btrim(nome_fantasia)) >= 3),
    descricao varchar(2000),
    whatsapp varchar(15) NOT NULL CHECK (whatsapp ~ '^[1-9][0-9]{9,14}$'),
    cidade varchar(100) NOT NULL CHECK (length(btrim(cidade)) >= 2),
    estado varchar(2) NOT NULL CHECK (estado IN ('AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO')),
    situacao varchar(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','APROVADO','BLOQUEADO')),
    motivo_moderacao varchar(500),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    atualizado_em timestamptz NOT NULL DEFAULT current_timestamp,
    versao bigint NOT NULL DEFAULT 0,
    CHECK (situacao <> 'BLOQUEADO' OR length(btrim(motivo_moderacao)) >= 3 AND motivo_moderacao IS NOT NULL)
);
CREATE INDEX idx_fornecedor_situacao ON compara_preco.fornecedor (situacao);
CREATE INDEX idx_fornecedor_regiao ON compara_preco.fornecedor (estado, cidade);

CREATE TABLE compara_preco.oferta (
    id uuid PRIMARY KEY,
    fornecedor_id uuid NOT NULL REFERENCES compara_preco.fornecedor(id),
    variante_id uuid NOT NULL REFERENCES compara_preco.variante_produto(id),
    preco numeric NOT NULL CHECK (preco > 0 AND preco <= 9999999999.99 AND scale(preco) <= 2),
    estoque integer NOT NULL CHECK (estoque BETWEEN 0 AND 1000000000),
    disponivel boolean NOT NULL,
    moderacao varchar(20) NOT NULL DEFAULT 'PENDENTE' CHECK (moderacao IN ('PENDENTE','APROVADA','BLOQUEADA')),
    motivo_moderacao varchar(500),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    atualizado_em timestamptz NOT NULL DEFAULT current_timestamp,
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (fornecedor_id, variante_id),
    CHECK (moderacao <> 'BLOQUEADA' OR length(btrim(motivo_moderacao)) >= 3 AND motivo_moderacao IS NOT NULL)
);
CREATE INDEX idx_oferta_variante_preco ON compara_preco.oferta (variante_id, preco, id);
CREATE INDEX idx_oferta_moderacao ON compara_preco.oferta (moderacao);

-- A comparação e a contagem de ofertas usarão a mesma regra de publicação.
CREATE VIEW compara_preco.oferta_elegivel AS
SELECT o.* FROM compara_preco.oferta o
JOIN compara_preco.fornecedor f ON f.id = o.fornecedor_id
JOIN compara_preco.usuario u ON u.id = f.usuario_id
JOIN compara_preco.variante_produto v ON v.id = o.variante_id
JOIN compara_preco.produto p ON p.id = v.produto_id
JOIN compara_preco.categoria c ON c.id = p.categoria_id
WHERE o.moderacao = 'APROVADA' AND o.disponivel AND o.estoque > 0
    AND f.situacao = 'APROVADO' AND u.ativo AND u.perfil = 'FORNECEDOR'
    AND (NOT c.exige_armazenamento OR v.armazenamento_gb IS NOT NULL)
    AND (NOT c.exige_cor OR v.cor IS NOT NULL)
    AND NOT EXISTS (
        SELECT 1 FROM jsonb_array_elements_text(c.atributos_obrigatorios) AS requisito(chave)
        WHERE NOT jsonb_exists(v.atributos, requisito.chave)
    );
