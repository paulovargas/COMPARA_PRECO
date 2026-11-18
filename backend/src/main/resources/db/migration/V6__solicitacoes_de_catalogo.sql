CREATE TABLE compara_preco.solicitacao_catalogo (
    id uuid PRIMARY KEY,
    fornecedor_id uuid NOT NULL REFERENCES compara_preco.fornecedor(id) ON DELETE CASCADE,
    nome_produto varchar(160) NOT NULL CHECK (length(btrim(nome_produto)) >= 2),
    marca varchar(100) NOT NULL CHECK (length(btrim(marca)) >= 2),
    modelo varchar(120) NOT NULL CHECK (length(btrim(modelo)) >= 2),
    categoria varchar(100) NOT NULL CHECK (length(btrim(categoria)) >= 2),
    caracteristicas varchar(2000) NOT NULL CHECK (length(btrim(caracteristicas)) >= 10),
    situacao varchar(20) NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','ATENDIDA','RECUSADA')),
    motivo varchar(500),
    variante_id uuid REFERENCES compara_preco.variante_produto(id),
    criado_em timestamptz NOT NULL DEFAULT current_timestamp,
    atualizado_em timestamptz NOT NULL DEFAULT current_timestamp,
    versao bigint NOT NULL DEFAULT 0,
    CHECK (situacao <> 'ATENDIDA' OR variante_id IS NOT NULL),
    CHECK (situacao <> 'RECUSADA' OR (motivo IS NOT NULL AND length(btrim(motivo)) >= 3))
);
CREATE INDEX idx_solicitacao_catalogo_empresa ON compara_preco.solicitacao_catalogo (fornecedor_id, criado_em DESC, id);
CREATE INDEX idx_solicitacao_catalogo_pendentes ON compara_preco.solicitacao_catalogo (fornecedor_id) WHERE situacao='PENDENTE';
