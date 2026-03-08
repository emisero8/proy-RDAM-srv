-- =============================================================
-- RDAM — Script DDL Completo (Idempotente)
-- Registro de Deudores Alimentarios Morosos
-- Alumno: Serovich Emilio — DNI 43.770.166 — 3º T.S "A"
-- i2T Software Factory · Campus de Verano 2026
-- PostgreSQL 15+
-- =============================================================
-- Este script es idempotente: puede ejecutarse múltiples veces
-- sin generar errores. Cada ejecución recrea el esquema limpio.
-- =============================================================

BEGIN;

-- -------------------------------------------------------------
-- 0. LIMPIEZA (orden inverso por dependencias FK)
-- -------------------------------------------------------------
DROP TABLE IF EXISTS refresh_tokens  CASCADE;
DROP TABLE IF EXISTS certificados    CASCADE;
DROP TABLE IF EXISTS pagos           CASCADE;
DROP TABLE IF EXISTS historial_estados CASCADE;
DROP TABLE IF EXISTS solicitudes     CASCADE;
DROP TABLE IF EXISTS usuarios        CASCADE;

DROP VIEW  IF EXISTS v_bandeja_interna;

DROP SEQUENCE IF EXISTS seq_solicitud_numero;

DROP FUNCTION IF EXISTS fn_set_updated_at()           CASCADE;
DROP FUNCTION IF EXISTS fn_generar_numero_solicitud()  CASCADE;
DROP FUNCTION IF EXISTS fn_registrar_historial()       CASCADE;
DROP FUNCTION IF EXISTS fn_expirar_certificados()      CASCADE;

DROP TYPE IF EXISTS portal_login;
DROP TYPE IF EXISTS tipo_usuario;
DROP TYPE IF EXISTS rol_usuario;
DROP TYPE IF EXISTS urgencia_sol;
DROP TYPE IF EXISTS estado_sol;
DROP TYPE IF EXISTS estado_pago;
DROP TYPE IF EXISTS tipo_cert_enum;

-- -------------------------------------------------------------
-- 1. EXTENSIONES
-- -------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- UUIDs opcionales
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- Hashing de passwords
CREATE EXTENSION IF NOT EXISTS "unaccent";    -- Búsqueda sin acentos

-- -------------------------------------------------------------
-- 2. TIPOS ENUMERADOS
-- -------------------------------------------------------------

CREATE TYPE tipo_usuario   AS ENUM ('CIUDADANO', 'INTERNO');
CREATE TYPE rol_usuario    AS ENUM ('CIUDADANO', 'GESTOR', 'ADMIN');
CREATE TYPE urgencia_sol   AS ENUM ('NORMAL', 'URGENTE');
CREATE TYPE estado_sol     AS ENUM (
    'PENDIENTE_REVISION',
    'EN_REVISION',
    'APROBADA',
    'RECHAZADA',
    'PENDIENTE_PAGO',
    'PAGADA',
    'EMITIDA',
    'EXPIRADA'
);
CREATE TYPE estado_pago    AS ENUM ('PENDIENTE', 'PAGADO', 'RECHAZADO', 'REEMBOLSADO');
CREATE TYPE tipo_cert_enum AS ENUM (
    'LIBRE_DEUDA'
);
CREATE TYPE portal_login  AS ENUM ('CIUDADANO', 'ADMIN');

-- -------------------------------------------------------------
-- 3. SECUENCIA: numeración de solicitudes (concurrency-safe)
-- -------------------------------------------------------------
CREATE SEQUENCE seq_solicitud_numero
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

COMMENT ON SEQUENCE seq_solicitud_numero IS
    'Secuencia para generar números de solicitud de forma segura bajo concurrencia';

-- -------------------------------------------------------------
-- 4. TABLA: usuarios
-- -------------------------------------------------------------
CREATE TABLE usuarios (
    id          SERIAL          PRIMARY KEY,
    nombre      VARCHAR(100)    NOT NULL,
    apellido    VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL UNIQUE,
    password_hash TEXT          NOT NULL,
    dni_cuil    VARCHAR(20)     UNIQUE,
    telefono    VARCHAR(30),
    domicilio   TEXT,
    tipo        tipo_usuario    NOT NULL DEFAULT 'CIUDADANO',
    rol         rol_usuario     NOT NULL DEFAULT 'CIUDADANO',
    activo      BOOLEAN         NOT NULL DEFAULT TRUE,
    ultimo_acceso TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_tipo_rol CHECK (
        (tipo = 'CIUDADANO' AND rol = 'CIUDADANO') OR
        (tipo = 'INTERNO'   AND rol IN ('GESTOR', 'ADMIN'))
    )
);

COMMENT ON TABLE  usuarios              IS 'Usuarios del sistema: ciudadanos y personal interno';
COMMENT ON COLUMN usuarios.tipo         IS 'CIUDADANO = usuario público, INTERNO = empleado municipal';
COMMENT ON COLUMN usuarios.rol          IS 'Rol funcional que determina los permisos en el sistema';
COMMENT ON COLUMN usuarios.password_hash IS 'Hash bcrypt de la contraseña (cost factor 12)';

-- -------------------------------------------------------------
-- 5. TABLA: solicitudes
-- -------------------------------------------------------------
CREATE TABLE solicitudes (
    id              SERIAL          PRIMARY KEY,
    numero          VARCHAR(20)     NOT NULL UNIQUE,   -- SOL-YYYY-NNN
    ciudadano_id    INT             NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    revisor_id      INT             REFERENCES usuarios(id) ON DELETE SET NULL,
    tipo_cert       tipo_cert_enum  NOT NULL DEFAULT 'LIBRE_DEUDA',
    urgencia        urgencia_sol    NOT NULL DEFAULT 'NORMAL',
    estado          estado_sol      NOT NULL DEFAULT 'PENDIENTE_REVISION',
    arancel         DECIMAL(10,2)   CHECK (arancel >= 0),
    observaciones   TEXT,
    motivo_rechazo  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_numero_formato CHECK (numero ~ '^SOL-\d{4}-\d{3,6}$')
);

COMMENT ON TABLE  solicitudes              IS 'Solicitudes de certificados digitales';
COMMENT ON COLUMN solicitudes.numero       IS 'Número único de solicitud en formato SOL-YYYY-NNN';
COMMENT ON COLUMN solicitudes.motivo_rechazo IS 'Obligatorio cuando estado = RECHAZADA';

-- -------------------------------------------------------------
-- 6. TABLA: historial_estados
-- -------------------------------------------------------------
CREATE TABLE historial_estados (
    id              SERIAL          PRIMARY KEY,
    solicitud_id    INT             NOT NULL REFERENCES solicitudes(id) ON DELETE CASCADE,
    usuario_id      INT             REFERENCES usuarios(id) ON DELETE SET NULL,
    estado_anterior estado_sol,
    estado_nuevo    estado_sol      NOT NULL,
    comentario      TEXT,
    ip_origen       INET,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE historial_estados IS 'Auditoría completa de todos los cambios de estado de solicitudes';

-- -------------------------------------------------------------
-- 7. TABLA: pagos
-- -------------------------------------------------------------
CREATE TABLE pagos (
    id              SERIAL          PRIMARY KEY,
    solicitud_id    INT             NOT NULL REFERENCES solicitudes(id) ON DELETE RESTRICT,
    monto           DECIMAL(10,2)   NOT NULL CHECK (monto > 0),
    moneda          CHAR(3)         NOT NULL DEFAULT 'ARS',
    referencia_pp   VARCHAR(100)    UNIQUE,            -- ID de transacción PlusPagos
    checkout_token  VARCHAR(255),                      -- Token de sesión de checkout
    estado          estado_pago     NOT NULL DEFAULT 'PENDIENTE',
    webhook_payload JSONB,                             -- Payload completo del webhook
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  pagos                  IS 'Registro de pagos de aranceles via PlusPagos';
COMMENT ON COLUMN pagos.referencia_pp    IS 'ID de transacción devuelto por PlusPagos';
COMMENT ON COLUMN pagos.webhook_payload  IS 'Payload completo del webhook para auditoría y reprocesamiento';

-- -------------------------------------------------------------
-- 8. TABLA: certificados
-- -------------------------------------------------------------
CREATE TABLE certificados (
    id              SERIAL          PRIMARY KEY,
    solicitud_id    INT             NOT NULL UNIQUE REFERENCES solicitudes(id) ON DELETE RESTRICT,
    emisor_id       INT             NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    archivo_url     TEXT            NOT NULL,           -- URL en Object Storage (S3/MinIO)
    archivo_hash    VARCHAR(64),                        -- SHA-256 del PDF para integridad
    firma_digital   TEXT,                               -- Firma PKCS#7 en Base64
    fecha_vencimiento DATE          NOT NULL,           -- created_at + 90 días por defecto
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  certificados               IS 'Certificados digitales emitidos';
COMMENT ON COLUMN certificados.archivo_url   IS 'URL presignada o path en Object Storage';
COMMENT ON COLUMN certificados.archivo_hash  IS 'SHA-256 del PDF para verificación de integridad';
COMMENT ON COLUMN certificados.firma_digital IS 'Firma PKCS#7 detached en Base64, emitida por PKI institucional';

-- -------------------------------------------------------------
-- 9. TABLA: refresh_tokens  (sesiones JWT)
-- -------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          SERIAL          PRIMARY KEY,
    usuario_id  INT             NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)     NOT NULL UNIQUE,       -- SHA-256 del refresh token
    login_portal portal_login   NOT NULL DEFAULT 'CIUDADANO', -- Portal desde el que se autenticó
    user_agent  TEXT,
    ip_origen   INET,
    expires_at  TIMESTAMPTZ     NOT NULL,
    revocado    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  refresh_tokens IS 'Refresh tokens JWT activos por usuario (permite revocación)';
COMMENT ON COLUMN refresh_tokens.login_portal IS 'Portal de login utilizado: CIUDADANO = ID Ciudadano, ADMIN = Login Interno';

-- -------------------------------------------------------------
-- 10. ÍNDICES
-- -------------------------------------------------------------

-- usuarios
CREATE INDEX idx_usuarios_email    ON usuarios(email);
CREATE INDEX idx_usuarios_dni_cuil ON usuarios(dni_cuil);
CREATE INDEX idx_usuarios_tipo_rol ON usuarios(tipo, rol);
CREATE INDEX idx_usuarios_activo   ON usuarios(activo) WHERE activo = TRUE;

-- solicitudes
CREATE INDEX idx_sol_ciudadano     ON solicitudes(ciudadano_id);
CREATE INDEX idx_sol_revisor       ON solicitudes(revisor_id);
CREATE INDEX idx_sol_estado        ON solicitudes(estado);
CREATE INDEX idx_sol_created       ON solicitudes(created_at DESC);
CREATE INDEX idx_sol_tipo_cert     ON solicitudes(tipo_cert);
-- Búsqueda combinada frecuente: bandeja interna
CREATE INDEX idx_sol_estado_created ON solicitudes(estado, created_at DESC);

-- historial_estados
CREATE INDEX idx_hist_solicitud    ON historial_estados(solicitud_id);
CREATE INDEX idx_hist_created      ON historial_estados(created_at DESC);

-- pagos
CREATE INDEX idx_pagos_solicitud   ON pagos(solicitud_id);
CREATE INDEX idx_pagos_referencia  ON pagos(referencia_pp);
CREATE INDEX idx_pagos_estado      ON pagos(estado);

-- certificados
CREATE INDEX idx_cert_solicitud    ON certificados(solicitud_id);
CREATE INDEX idx_cert_emisor       ON certificados(emisor_id);
CREATE INDEX idx_cert_vencimiento  ON certificados(fecha_vencimiento);

-- refresh_tokens
CREATE INDEX idx_rt_usuario        ON refresh_tokens(usuario_id);
CREATE INDEX idx_rt_expires        ON refresh_tokens(expires_at) WHERE revocado = FALSE;
CREATE INDEX idx_rt_login_portal   ON refresh_tokens(login_portal);

-- -------------------------------------------------------------
-- 11. FUNCIÓN: updated_at automático
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Triggers updated_at
CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_solicitudes_updated_at
    BEFORE UPDATE ON solicitudes
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_pagos_updated_at
    BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -------------------------------------------------------------
-- 12. FUNCIÓN: generar número de solicitud (concurrency-safe)
-- -------------------------------------------------------------
-- Usa una secuencia dedicada en lugar de COUNT(*) para evitar
-- race conditions bajo inserciones concurrentes.
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_generar_numero_solicitud()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_year  INT  := EXTRACT(YEAR FROM NOW());
    v_seq   INT;
    v_num   TEXT;
BEGIN
    v_seq := nextval('seq_solicitud_numero');
    v_num := 'SOL-' || v_year || '-' || LPAD(v_seq::TEXT, 3, '0');
    NEW.numero := v_num;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_solicitudes_numero
    BEFORE INSERT ON solicitudes
    FOR EACH ROW
    WHEN (NEW.numero IS NULL OR NEW.numero = '')
    EXECUTE FUNCTION fn_generar_numero_solicitud();

-- -------------------------------------------------------------
-- 13. FUNCIÓN: registrar historial automáticamente
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_registrar_historial()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado IS DISTINCT FROM NEW.estado THEN
        INSERT INTO historial_estados (solicitud_id, estado_anterior, estado_nuevo)
        VALUES (NEW.id, OLD.estado, NEW.estado);
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_solicitudes_historial
    AFTER UPDATE OF estado ON solicitudes
    FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial();

-- -------------------------------------------------------------
-- 14. FUNCIÓN: expirar certificados automáticamente
-- -------------------------------------------------------------
-- Ejecutar periódicamente via pg_cron o job externo.
-- Ejemplo con pg_cron:
--   SELECT cron.schedule('expire-certs', '0 3 * * *',
--          'SELECT fn_expirar_certificados()');
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_expirar_certificados()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    UPDATE solicitudes
       SET estado = 'EXPIRADA'
     WHERE estado = 'EMITIDA'
       AND id IN (
           SELECT solicitud_id FROM certificados
            WHERE fecha_vencimiento < CURRENT_DATE
       );
END;
$$;

COMMENT ON FUNCTION fn_expirar_certificados IS
    'Ejecutar periódicamente via pg_cron o job externo para marcar certificados vencidos';

-- -------------------------------------------------------------
-- 15. VISTA: bandeja interna (solicitudes activas)
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_bandeja_interna AS
SELECT
    s.id,
    s.numero,
    s.estado,
    s.tipo_cert,
    s.urgencia,
    s.arancel,
    s.created_at,
    s.updated_at,
    u_c.nombre    || ' ' || u_c.apellido AS ciudadano_nombre,
    u_c.dni_cuil  AS ciudadano_cuil,
    u_c.email     AS ciudadano_email,
    u_r.nombre    || ' ' || u_r.apellido AS revisor_nombre
FROM solicitudes s
JOIN usuarios u_c ON u_c.id = s.ciudadano_id
LEFT JOIN usuarios u_r ON u_r.id = s.revisor_id
WHERE s.estado NOT IN ('RECHAZADA', 'EXPIRADA');

-- -------------------------------------------------------------
-- 16. DATOS SEMILLA (seed)
-- -------------------------------------------------------------

-- Admin por defecto (password: Admin1234! — cambiar en producción)
-- Login via portal ADMIN (/auth/admin/login)
INSERT INTO usuarios (nombre, apellido, email, password_hash, tipo, rol, dni_cuil)
VALUES (
    'Admin', 'Sistema',
    'admin@rdam.gob.ar',
    crypt('Admin1234!', gen_salt('bf', 12)),
    'INTERNO', 'ADMIN',
    '20-00000001-0'
);

-- Gestor de ejemplo
-- Login via portal ADMIN (/auth/admin/login)
INSERT INTO usuarios (nombre, apellido, email, password_hash, tipo, rol, dni_cuil)
VALUES (
    'Laura', 'Martínez',
    'lmartinez@rdam.gob.ar',
    crypt('Gestor1234!', gen_salt('bf', 12)),
    'INTERNO', 'GESTOR',
    '27-12345678-9'
);

-- Ciudadano de ejemplo
-- Login via portal CIUDADANO (/auth/login)
INSERT INTO usuarios (nombre, apellido, email, password_hash, tipo, rol, dni_cuil)
VALUES (
    'María', 'García',
    'mgarcia@email.com',
    crypt('Ciudadano1234!', gen_salt('bf', 12)),
    'CIUDADANO', 'CIUDADANO',
    '27-34567890-1'
);

COMMIT;

-- =============================================================
-- FIN DEL SCRIPT DDL
-- Tablas: usuarios, solicitudes, historial_estados, pagos,
--         certificados, refresh_tokens
-- Vista:  v_bandeja_interna
-- Triggers: updated_at, número solicitud, historial automático
-- Funciones: expiración de certificados
-- =============================================================
