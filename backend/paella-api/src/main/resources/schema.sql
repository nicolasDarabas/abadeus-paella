-- =========================================================
--  PAELLA BENEFICENTE ABADEUS - Script do banco (SQLite)
--  Executado automaticamente ao iniciar a API.
--  Todas as tabelas possuem os campos de auditoria:
--  criado_em, atualizado_em, criado_por, atualizado_por
-- =========================================================

CREATE TABLE IF NOT EXISTS perfil_usuario (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    nome            TEXT    NOT NULL UNIQUE,
    descricao       TEXT,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS usuario (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    perfil_id       INTEGER NOT NULL REFERENCES perfil_usuario(id),
    nome            TEXT    NOT NULL,
    login           TEXT    NOT NULL UNIQUE,
    email           TEXT    NOT NULL UNIQUE,
    telefone        TEXT,
    senha_hash      TEXT    NOT NULL,
    ativo           INTEGER NOT NULL DEFAULT 1,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS evento (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo          TEXT    NOT NULL,
    descricao       TEXT,
    local           TEXT    NOT NULL,
    data_evento     TIMESTAMP NOT NULL,
    retirada_inicio TIMESTAMP,
    retirada_fim    TIMESTAMP,
    contato         TEXT,
    regras          TEXT,
    status          TEXT    NOT NULL,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS produto (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    evento_id       INTEGER NOT NULL REFERENCES evento(id),
    nome            TEXT    NOT NULL,
    descricao       TEXT,
    preco           NUMERIC NOT NULL,
    limite_estoque  INTEGER,
    ativo           INTEGER NOT NULL DEFAULT 1,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS lote (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    evento_id       INTEGER NOT NULL REFERENCES evento(id),
    nome            TEXT    NOT NULL,
    venda_inicio    TIMESTAMP NOT NULL,
    venda_fim       TIMESTAMP NOT NULL,
    ativo           INTEGER NOT NULL DEFAULT 1,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS lote_produto (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    lote_id         INTEGER NOT NULL REFERENCES lote(id),
    produto_id      INTEGER NOT NULL REFERENCES produto(id),
    preco_lote      NUMERIC NOT NULL,
    quantidade      INTEGER,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT,
    UNIQUE (lote_id, produto_id)
);

CREATE TABLE IF NOT EXISTS pedido (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    evento_id       INTEGER NOT NULL REFERENCES evento(id),
    usuario_id      INTEGER NOT NULL REFERENCES usuario(id),
    numero          TEXT    NOT NULL UNIQUE,
    origem          TEXT    NOT NULL,
    status          TEXT    NOT NULL,
    valor_total     NUMERIC NOT NULL,
    codigo_retirada TEXT    UNIQUE,
    expira_em       TIMESTAMP,
    observacao      TEXT,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS pedido_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id       INTEGER NOT NULL REFERENCES pedido(id),
    produto_id      INTEGER NOT NULL REFERENCES produto(id),
    lote_id         INTEGER REFERENCES lote(id),
    quantidade      INTEGER NOT NULL,
    preco_unitario  NUMERIC NOT NULL,
    subtotal        NUMERIC NOT NULL,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS pagamento (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id       INTEGER NOT NULL REFERENCES pedido(id),
    metodo          TEXT    NOT NULL,
    valor           NUMERIC NOT NULL,
    status          TEXT    NOT NULL,
    id_externo      TEXT,
    codigo_pix      TEXT,
    confirmado_em   TIMESTAMP,
    confirmado_por  TEXT,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE TABLE IF NOT EXISTS pedido_status_historico (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id       INTEGER NOT NULL REFERENCES pedido(id),
    status_anterior TEXT,
    status_novo     TEXT    NOT NULL,
    motivo          TEXT,
    alterado_por    TEXT,
    alterado_em     TIMESTAMP NOT NULL,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

-- pedido_id UNIQUE garante a RETIRADA UNICA: impossivel retirar duas vezes
CREATE TABLE IF NOT EXISTS retirada (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id            INTEGER NOT NULL UNIQUE REFERENCES pedido(id),
    validado_por         TEXT    NOT NULL,
    validado_em          TIMESTAMP NOT NULL,
    metodo_validacao     TEXT    NOT NULL,
    sincronizado_offline INTEGER NOT NULL DEFAULT 0,
    criado_em            TIMESTAMP,
    atualizado_em        TIMESTAMP,
    criado_por           TEXT,
    atualizado_por       TEXT
);

CREATE TABLE IF NOT EXISTS comprovante_retirada (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    retirada_id     INTEGER NOT NULL UNIQUE REFERENCES retirada(id),
    codigo          TEXT    NOT NULL UNIQUE,
    emitido_em      TIMESTAMP NOT NULL,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

-- Avisos ao comprador (envio de e-mail SIMULADO)
CREATE TABLE IF NOT EXISTS notificacao (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id      INTEGER NOT NULL REFERENCES usuario(id),
    pedido_id       INTEGER REFERENCES pedido(id),
    tipo            TEXT    NOT NULL,
    canal           TEXT    NOT NULL,
    destinatario    TEXT    NOT NULL,
    assunto         TEXT    NOT NULL,
    mensagem        TEXT    NOT NULL,
    enviada_em      TIMESTAMP NOT NULL,
    status          TEXT    NOT NULL,
    criado_em       TIMESTAMP,
    atualizado_em   TIMESTAMP,
    criado_por      TEXT,
    atualizado_por  TEXT
);

CREATE INDEX IF NOT EXISTS idx_notificacao_usuario ON notificacao(usuario_id);
CREATE INDEX IF NOT EXISTS idx_produto_evento   ON produto(evento_id);
CREATE INDEX IF NOT EXISTS idx_lote_evento      ON lote(evento_id);
CREATE INDEX IF NOT EXISTS idx_pedido_evento    ON pedido(evento_id);
CREATE INDEX IF NOT EXISTS idx_pedido_usuario   ON pedido(usuario_id);
CREATE INDEX IF NOT EXISTS idx_pedido_status    ON pedido(status);
CREATE INDEX IF NOT EXISTS idx_item_pedido      ON pedido_item(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pagamento_pedido ON pagamento(pedido_id);
CREATE INDEX IF NOT EXISTS idx_historico_pedido ON pedido_status_historico(pedido_id);

-- =========================================================
-- Atualizacao de bancos criados por versoes anteriores.
-- Em banco novo estas linhas falham (coluna ja existe) e sao ignoradas
-- (spring.sql.init.continue-on-error=true).
-- =========================================================
ALTER TABLE evento ADD COLUMN contato TEXT;
ALTER TABLE evento ADD COLUMN regras TEXT;
