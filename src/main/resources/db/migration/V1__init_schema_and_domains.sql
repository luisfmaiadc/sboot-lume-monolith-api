-- ============================================================================
-- PROJETO Lume — SCRIPT DDL CONSOLIDADO V1 (PostgreSQL 16+ com PostGIS)
-- ============================================================================

-- 1. EXTENSÕES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 2. TIPOS ENUMERADOS (Fluxos e Estados Técnicos)
CREATE TYPE enum_genero AS ENUM (
    'MULHER_CIS', 'HOMEM_CIS', 'MULHER_TRANS', 'HOMEM_TRANS',
    'NAO_BINARIO', 'OUTRO', 'PREFIRO_NAO_INFORMAR'
);

CREATE TYPE enum_raca_etnia AS ENUM (
    'BRANCA', 'PRETA', 'PARDA', 'INDIGENA', 'AMARELA', 'PREFIRO_NAO_INFORMAR'
);

CREATE TYPE enum_orientacao_sexual AS ENUM (
    'HETEROSSEXUAL', 'HOMOSSEXUAL', 'BISSEXUAL', 'PANSEXUAL', 'ASSEXUAL', 'OUTRO', 'PREFIRO_NAO_INFORMAR'
);

CREATE TYPE enum_faixa_horario AS ENUM (
    'MADRUGADA', 'MANHA', 'TARDE', 'NOITE'
);

CREATE TYPE enum_tipo_ambiente AS ENUM (
    'ESTABELECIMENTO', 'VIA_PUBLICA', 'TRANSPORTE_PUBLICO', 'PARQUE_PRACA', 'OUTRO'
);

CREATE TYPE enum_status_ocorrencia AS ENUM (
    'PUBLICADA', 'SOB_ANALISE', 'REMOVIDA'
);

CREATE TYPE enum_tipo_validacao AS ENUM (
    'PRESENCIEI', 'CONHECO_RISCO'
);

-- 3. TABELAS: IDENTIDADE E ACESSO

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome_completo VARCHAR(150) NOT NULL,
    cpf_hash VARCHAR(64) NOT NULL UNIQUE,
    cpf_cifrado VARCHAR(512) NOT NULL,
    telefone VARCHAR(20) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    genero enum_genero NOT NULL DEFAULT 'PREFIRO_NAO_INFORMAR',
    raca_etnia enum_raca_etnia NOT NULL DEFAULT 'PREFIRO_NAO_INFORMAR',
    orientacao_sexual enum_orientacao_sexual NOT NULL DEFAULT 'PREFIRO_NAO_INFORMAR',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID,
    atualizado_em TIMESTAMPTZ,
    atualizado_por UUID,
    CONSTRAINT chk_telefone_formato CHECK (telefone ~ '^[0-9]{10,11}$')
);

CREATE TABLE credenciais (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    hash_senha VARCHAR(255) NOT NULL,
    tentativas_falhas INTEGER NOT NULL DEFAULT 0,
    bloqueado_ate TIMESTAMPTZ,
    ultimo_login_em TIMESTAMPTZ,
    requer_redefinicao_senha BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ,
    CONSTRAINT fk_credenciais_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE perfis (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

INSERT INTO perfis (nome, descricao) VALUES
    ('ROLE_USUARIO', 'Acesso de usuário comum'),
    ('ROLE_GESTOR', 'Gestor autorizado de estabelecimento comercial'),
    ('ROLE_MODERADOR', 'Moderação de conteúdo e denúncias'),
    ('ROLE_ADMIN', 'Administrador completo da plataforma');

CREATE TABLE usuarios_perfis (
    usuario_id UUID NOT NULL,
    perfil_id INTEGER NOT NULL,
    atribuido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atribuido_por UUID,
    PRIMARY KEY (usuario_id, perfil_id),
    CONSTRAINT fk_usuarios_perfis_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuarios_perfis_perfil FOREIGN KEY (perfil_id) 
        REFERENCES perfis(id) ON DELETE RESTRICT
);

-- 4. TABELAS: DOMÍNIOS DE CONFIGURAÇÃO

CREATE TABLE categorias_estabelecimentos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO categorias_estabelecimentos (codigo, nome) VALUES
    ('BAR_RESTAURANTE', 'Bares e Restaurantes'),
    ('CASA_NOTURNA_SHOW', 'Casas Noturnas e Shows'),
    ('SHOPPING_COMERCIO', 'Shoppings e Centros Comerciais'),
    ('INSTITUICAO_ENSINO', 'Faculdades e Escolas'),
    ('ACADEMIA_ESPORTE', 'Academias e Clubes'),
    ('TRANSPORTE_TERMINAL', 'Estações e Terminais'),
    ('HOSPITAL_SAUDE', 'Hospitais e Unidades de Saúde'),
    ('OUTRO', 'Outros Estabelecimentos');

CREATE TABLE tipos_violencia (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    peso_gravidade INTEGER NOT NULL,
    ordem_exibicao INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_peso_gravidade CHECK (peso_gravidade BETWEEN 1 AND 10)
);

INSERT INTO tipos_violencia (codigo, nome, peso_gravidade, ordem_exibicao) VALUES
    ('ASSEDIO_VERBAL', 'Assédio Verbal / Importunação', 2, 1),
    ('IMPORTUNACAO_SEXUAL', 'Importunação Sexual', 6, 2),
    ('PERSEGUICAO_STALKING', 'Perseguição (Stalking)', 5, 3),
    ('AGRESSAO_VERBAL_AMEACA', 'Ameaça e Intimidação Verbal', 4, 4),
    ('AGRESSAO_FISICA', 'Agressão Física', 8, 5),
    ('ROUBO_FURTO', 'Roubo / Furto', 6, 6),
    ('DISCRIMINACAO', 'Discriminação / Preconceito', 5, 7),
    ('ESTUPRO_VIOLENCIA_SEXUAL', 'Estupro / Violência Sexual', 10, 8),
    ('LATROCINIO', 'Latrocínio (Roubo seguido de Morte)', 10, 9),
    ('HOMICIDIO_OU_TENTATIVA', 'Homicídio ou Tentativa', 10, 10),
    ('OUTRO', 'Outro Tipo de Violência', 3, 11);

CREATE TABLE marcadores_vulnerabilidade (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO marcadores_vulnerabilidade (codigo, nome, descricao) VALUES
    ('MISOGINIA', 'Violência contra a Mulher / Misoginia', 'Ataque motivado pelo fato de a vítima ser mulher.'),
    ('RACISMO', 'Racismo / Injúria Racial', 'Agressão motivada por cor, raça ou etnia.'),
    ('LGBTFOBIA', 'LGBTfobia / Homofobia / Transfobia', 'Violência motivada pela orientação sexual ou identidade de gênero.'),
    ('CAPACITISMO', 'Capacitismo', 'Discriminação contra pessoas com deficiência.'),
    ('INTOLERANCIA_RELIGIOSA', 'Intolerância Religiosa', 'Ataque motivado por crença ou manifestação religiosa.');

-- 5. TABELAS: ESTABELECIMENTOS E GESTÃO

CREATE TABLE estabelecimentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    categoria_id INTEGER NOT NULL,
    nome VARCHAR(150) NOT NULL,
    cnpj VARCHAR(14) UNIQUE,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    localizacao GEOGRAPHY(Point, 4326) NOT NULL,
    score_reputacao NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID,
    atualizado_em TIMESTAMPTZ,
    atualizado_por UUID,
    CONSTRAINT fk_estabelecimentos_categoria FOREIGN KEY (categoria_id) 
        REFERENCES categorias_estabelecimentos(id) ON DELETE RESTRICT,
    CONSTRAINT chk_score_range CHECK (score_reputacao BETWEEN 0.00 AND 5.00)
);

CREATE TABLE estabelecimentos_gestores (
    estabelecimento_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    cargo VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    vinculado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vinculado_por UUID,
    PRIMARY KEY (estabelecimento_id, usuario_id),
    CONSTRAINT fk_gestores_estabelecimento FOREIGN KEY (estabelecimento_id) 
        REFERENCES estabelecimentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_gestores_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 6. TABELAS: OCORRÊNCIAS E VALIDAÇÕES

CREATE TABLE ocorrencias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    estabelecimento_id UUID,
    localizacao GEOGRAPHY(Point, 4326) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    data_ocorrencia DATE NOT NULL,
    faixa_horario enum_faixa_horario NOT NULL,
    tipo_violencia_id INTEGER NOT NULL,
    marcador_vulnerabilidade_id INTEGER,
    tipo_ambiente enum_tipo_ambiente NOT NULL,
    total_validacoes INTEGER NOT NULL DEFAULT 0,
    status enum_status_ocorrencia NOT NULL DEFAULT 'PUBLICADA',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID NOT NULL,
    atualizado_em TIMESTAMPTZ,
    atualizado_por UUID,
    CONSTRAINT fk_ocorrencias_estabelecimento FOREIGN KEY (estabelecimento_id) 
        REFERENCES estabelecimentos(id) ON DELETE SET NULL,
    CONSTRAINT fk_ocorrencias_tipo_violencia FOREIGN KEY (tipo_violencia_id) 
        REFERENCES tipos_violencia(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ocorrencias_marcador FOREIGN KEY (marcador_vulnerabilidade_id) 
        REFERENCES marcadores_vulnerabilidade(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ocorrencias_criador FOREIGN KEY (criado_por) 
        REFERENCES usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ocorrencias_atualizador FOREIGN KEY (atualizado_por) 
        REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE validacoes_ocorrencias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ocorrencia_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    tipo_validacao enum_tipo_validacao NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por UUID NOT NULL,
    atualizado_em TIMESTAMPTZ,
    atualizado_por UUID,
    CONSTRAINT uk_ocorrencia_usuario UNIQUE (ocorrencia_id, usuario_id),
    CONSTRAINT fk_validacoes_ocorrencia FOREIGN KEY (ocorrencia_id) 
        REFERENCES ocorrencias(id) ON DELETE CASCADE,
    CONSTRAINT fk_validacoes_usuario FOREIGN KEY (usuario_id) 
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_validacoes_atualizador FOREIGN KEY (atualizado_por) 
        REFERENCES usuarios(id) ON DELETE SET NULL
);

-- 7. ÍNDICES DE PERFORMANCE E ESPACIAIS

CREATE INDEX idx_ocorrencias_localizacao_gist ON ocorrencias USING GIST (localizacao);
CREATE INDEX idx_estabelecimentos_localizacao_gist ON estabelecimentos USING GIST (localizacao);

CREATE INDEX idx_usuarios_cpf_hash ON usuarios(cpf_hash);
CREATE INDEX idx_usuarios_telefone ON usuarios(telefone);
CREATE INDEX idx_credenciais_email ON credenciais(email);
CREATE INDEX idx_estabelecimentos_bairro_cidade ON estabelecimentos(bairro, cidade);
CREATE INDEX idx_ocorrencias_bairro_cidade ON ocorrencias(bairro, cidade);
CREATE INDEX idx_ocorrencias_estabelecimento_id ON ocorrencias(estabelecimento_id);
CREATE INDEX idx_ocorrencias_status_data ON ocorrencias(status, data_ocorrencia DESC);
CREATE INDEX idx_ocorrencias_status_faixa_data ON ocorrencias(status, faixa_horario, data_ocorrencia DESC);
CREATE INDEX idx_ocorrencias_tipo_violencia ON ocorrencias(tipo_violencia_id);
CREATE INDEX idx_ocorrencias_marcador ON ocorrencias(marcador_vulnerabilidade_id);
CREATE INDEX idx_validacoes_ocorrencia_ativo ON validacoes_ocorrencias(ocorrencia_id, usuario_id, ativo);

-- 8. SPRING MODULITH OUTBOX & SHEDLOCK

CREATE TABLE event_publication (
    id UUID NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    status VARCHAR(64),
    completion_attempts INT DEFAULT 0,
    last_resubmission_date TIMESTAMPTZ
);

CREATE INDEX idx_event_publication_serialized_event ON event_publication (listener_id, serialized_event);
CREATE INDEX idx_event_publication_by_completion_date ON event_publication (completion_date);

CREATE TABLE event_publication_archive (
    id UUID NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    status VARCHAR(64),
    completion_attempts INT DEFAULT 0,
    last_resubmission_date TIMESTAMPTZ
);

CREATE INDEX idx_event_publication_archive_by_completion_date ON event_publication_archive (completion_date);

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
