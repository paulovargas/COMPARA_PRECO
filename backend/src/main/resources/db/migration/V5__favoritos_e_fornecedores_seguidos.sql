CREATE TABLE compara_preco.produto_favorito (
    usuario_id uuid NOT NULL REFERENCES compara_preco.usuario(id) ON DELETE CASCADE,
    produto_id uuid NOT NULL REFERENCES compara_preco.produto(id) ON DELETE CASCADE,
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY (usuario_id, produto_id)
);
CREATE INDEX idx_produto_favorito_pagina ON compara_preco.produto_favorito (usuario_id, criado_em DESC, produto_id);
CREATE INDEX idx_produto_favorito_produto ON compara_preco.produto_favorito (produto_id);

CREATE TABLE compara_preco.fornecedor_seguido (
    usuario_id uuid NOT NULL REFERENCES compara_preco.usuario(id) ON DELETE CASCADE,
    fornecedor_id uuid NOT NULL REFERENCES compara_preco.fornecedor(id) ON DELETE CASCADE,
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    PRIMARY KEY (usuario_id, fornecedor_id)
);
CREATE INDEX idx_fornecedor_seguido_pagina ON compara_preco.fornecedor_seguido (usuario_id, criado_em DESC, fornecedor_id);
CREATE INDEX idx_fornecedor_seguido_fornecedor ON compara_preco.fornecedor_seguido (fornecedor_id);
