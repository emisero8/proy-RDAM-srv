-- V3__remove_ciudadano_entity.sql

-- 1. Modificar tabla solicitudes
ALTER TABLE solicitudes
    DROP CONSTRAINT solicitudes_ciudadano_id_fkey;

ALTER TABLE solicitudes
    DROP COLUMN ciudadano_id;

ALTER TABLE solicitudes
    ADD COLUMN email VARCHAR(150),
    ADD COLUMN nombre VARCHAR(100),
    ADD COLUMN apellido VARCHAR(100),
    ADD COLUMN dni VARCHAR(20),
    ADD COLUMN fecha_nacimiento DATE;

-- Populate existing rows with dummy data to allow setting NOT NULL if we wanted, but we'll just leave them nullable or set defaults.
-- Let's make email required for backwards compatibility logic
UPDATE solicitudes SET 
    email = COALESCE(email, 'migrated@example.com'),
    nombre = COALESCE(nombre, 'Migrated'),
    apellido = COALESCE(apellido, 'Migrated'),
    dni = COALESCE(dni, '00000000'),
    fecha_nacimiento = COALESCE(fecha_nacimiento, '1900-01-01');
ALTER TABLE solicitudes
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN nombre SET NOT NULL,
    ALTER COLUMN apellido SET NOT NULL,
    ALTER COLUMN dni SET NOT NULL,
    ALTER COLUMN fecha_nacimiento SET NOT NULL;

CREATE INDEX idx_solicitudes_email ON solicitudes(email);

-- 2. Modificar tabla refresh_tokens
ALTER TABLE refresh_tokens
    ALTER COLUMN usuario_id DROP NOT NULL;

ALTER TABLE refresh_tokens
    ADD COLUMN email VARCHAR(150);

CREATE INDEX idx_refresh_tokens_email ON refresh_tokens(email);
