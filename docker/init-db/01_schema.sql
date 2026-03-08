-- ============================================================
-- RDAM — Inicialización de la base de datos en Docker
-- Este script es ejecutado automáticamente por PostgreSQL
-- la primera vez que se crea el contenedor de la BD.
-- ============================================================

-- ─── Tipos / Enums ─────────────────────────────────────────
DO $$ BEGIN
    CREATE TYPE tipo_usuario AS ENUM ('CIUDADANO','INTERNO');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE rol_usuario AS ENUM ('CIUDADANO','GESTOR','ADMIN');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

-- ─── Tablas ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    TEXT,
    dni_cuil    VARCHAR(20)  UNIQUE,
    telefono    VARCHAR(30),
    domicilio   TEXT,
    circunscripcion VARCHAR(100),
    tipo        VARCHAR(10)  NOT NULL CHECK (tipo IN ('CIUDADANO','INTERNO')),
    rol         VARCHAR(15)  NOT NULL CHECK (rol IN ('CIUDADANO','GESTOR','ADMIN')),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS solicitudes (
    id              BIGSERIAL    PRIMARY KEY,
    numero          VARCHAR(20)  NOT NULL UNIQUE,
    ciudadano_id    BIGINT       NOT NULL REFERENCES usuarios(id),
    tipo_cert       VARCHAR(50)  DEFAULT 'LIBRE_DEUDA',
    urgencia        VARCHAR(10)  DEFAULT 'NORMAL',
    estado          VARCHAR(25)  NOT NULL DEFAULT 'PENDIENTE_REVISION',
    arancel         NUMERIC(10,2),
    observaciones   TEXT,
    motivo_rechazo  TEXT,
    revisor_id      BIGINT       REFERENCES usuarios(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pagos (
    id           BIGSERIAL    PRIMARY KEY,
    solicitud_id BIGINT       NOT NULL REFERENCES solicitudes(id),
    monto        NUMERIC(10,2) NOT NULL,
    referencia   VARCHAR(50),
    checkout_url TEXT,
    estado       VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS certificados (
    id               BIGSERIAL  PRIMARY KEY,
    solicitud_id     BIGINT     NOT NULL UNIQUE REFERENCES solicitudes(id),
    archivo_url      TEXT       NOT NULL,
    emisor_id        BIGINT     REFERENCES usuarios(id),
    firma_digital    TEXT,
    fecha_vencimiento DATE,
    created_at       TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS historial_estados (
    id           BIGSERIAL  PRIMARY KEY,
    solicitud_id BIGINT     NOT NULL REFERENCES solicitudes(id),
    estado_ant   VARCHAR(25),
    estado_nuevo VARCHAR(25) NOT NULL,
    usuario_id   BIGINT     REFERENCES usuarios(id),
    comentario   TEXT,
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           BIGSERIAL  PRIMARY KEY,
    token        TEXT       NOT NULL UNIQUE,
    usuario_id   BIGINT     NOT NULL REFERENCES usuarios(id),
    expires_at   TIMESTAMP  NOT NULL,
    revocado     BOOLEAN    NOT NULL DEFAULT FALSE,
    login_portal VARCHAR(20),
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW()
);

-- ─── Datos semilla ──────────────────────────────────────────
-- Contraseña para todos: Password1!
INSERT INTO usuarios (nombre, apellido, email, password, dni_cuil, tipo, rol)
VALUES
    ('Admin',  'Sistema',  'admin@rdam.gob.ar',      '$2b$10$/qfmOBdlZw7iryK73cWS2.Cd3mL48vFXXhbDmW6mRGSiUJCaliKda', '20-00000001-0', 'INTERNO',   'ADMIN'),
    ('Laura',  'Martinez', 'lmartinez@rdam.gob.ar',  '$2b$10$/qfmOBdlZw7iryK73cWS2.Cd3mL48vFXXhbDmW6mRGSiUJCaliKda', '27-12345678-9', 'INTERNO',   'GESTOR'),
    ('Maria',  'Garcia',   'mgarcia@email.com',       '$2b$10$/qfmOBdlZw7iryK73cWS2.Cd3mL48vFXXhbDmW6mRGSiUJCaliKda', '27-34567890-1', 'CIUDADANO', 'CIUDADANO')
ON CONFLICT (email) DO NOTHING;
