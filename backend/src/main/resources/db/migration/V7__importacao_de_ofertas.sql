CREATE TABLE compara_preco.importacao_ofertas (
    id UUID PRIMARY KEY,
    fornecedor_id UUID NOT NULL REFERENCES compara_preco.fornecedor(id) ON DELETE CASCADE,
    criado_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    aplicada BOOLEAN NOT NULL DEFAULT FALSE,
    previa TEXT NOT NULL
);
CREATE INDEX idx_importacao_fornecedor ON compara_preco.importacao_ofertas(fornecedor_id, expira_em);
