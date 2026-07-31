-- ====================================================================
-- V4__help_tickets.sql
-- Tabla para tickets del Centro de Ayuda
-- ====================================================================

CREATE TABLE IF NOT EXISTS help_tickets (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(150) NOT NULL,
    mensaje    VARCHAR(256) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
