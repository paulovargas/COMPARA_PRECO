CREATE TABLE compara_preco.usuario (
    id uuid PRIMARY KEY,
    nome varchar(100) NOT NULL CHECK (length(btrim(nome)) >= 2),
    email varchar(254) NOT NULL UNIQUE CHECK (
        email = lower(btrim(email)) AND position('@' in email) > 1 AND email !~ '[[:space:]]'
    ),
    senha_hash varchar(255) NOT NULL CHECK (length(senha_hash) >= 60),
    perfil varchar(20) NOT NULL CHECK (perfil IN ('COMPRADOR', 'FORNECEDOR', 'ADMIN')),
    ativo boolean NOT NULL DEFAULT true,
    versao bigint NOT NULL DEFAULT 0,
    criado_em timestamptz NOT NULL DEFAULT current_timestamp
);

-- Estrutura do Spring Session JDBC, qualificada para não depender do search_path.
CREATE TABLE compara_preco.spring_session (
    primary_id char(36) NOT NULL PRIMARY KEY,
    session_id char(36) NOT NULL UNIQUE,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(254)
);
CREATE INDEX idx_spring_session_expiry ON compara_preco.spring_session (expiry_time);
CREATE INDEX idx_spring_session_principal ON compara_preco.spring_session (principal_name);
CREATE TABLE compara_preco.spring_session_attributes (
    session_primary_id char(36) NOT NULL REFERENCES compara_preco.spring_session(primary_id) ON DELETE CASCADE,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);

CREATE TABLE compara_preco.limite_acesso (
    chave char(64) PRIMARY KEY,
    janela_inicio timestamptz NOT NULL,
    quantidade integer NOT NULL CHECK (quantidade > 0)
);
CREATE INDEX idx_limite_acesso_janela ON compara_preco.limite_acesso (janela_inicio);
