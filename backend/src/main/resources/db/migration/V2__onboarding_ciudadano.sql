-- ====================================================================
-- V2__onboarding_ciudadano.sql
-- Agrega campos de onboarding a la tabla usuarios:
--   - fecha_nacimiento : para validación de mayoría de edad
--   - perfil_completo  : indica si el ciudadano completó su perfil
-- ====================================================================

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE,
    ADD COLUMN IF NOT EXISTS perfil_completo  BOOLEAN NOT NULL DEFAULT FALSE;

-- Marcar como completos los usuarios existentes que ya tienen datos reales
-- (nombre != 'Ciudadano' y apellido no vacío y dni_cuil cargado)
UPDATE usuarios
   SET perfil_completo = TRUE
 WHERE tipo = 'CIUDADANO'
   AND (nombre <> 'Ciudadano' OR dni_cuil IS NOT NULL);

-- Los usuarios INTERNOS siempre tienen perfil completo
UPDATE usuarios
   SET perfil_completo = TRUE
 WHERE tipo = 'INTERNO';
