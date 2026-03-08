# RDAM — Mapa de Endpoints REST API
## i2T Software Factory · Campus de Verano 2026
### Alumno: Serovich Emilio — DNI 43.770.166 — 3º T.S "A"
### Vinculado a DDL.sql

**Base URL:** `https://api.rdam.gob.ar/v1`  
**Autenticación:** Bearer Token (JWT RS256) en header `Authorization`  
**Formato:** JSON (`Content-Type: application/json`)

---

## Convenciones

| Símbolo | Significado |
|---------|-------------|
| 🔓 | Público — sin autenticación |
| 🔑 | Requiere JWT válido |
| 👤 | Solo rol CIUDADANO |
| 🏛️ | Rol GESTOR |
| ⚙️ | Solo rol ADMIN |
| 📊 | Rol ADMIN |

---

## 1. Autenticación `/auth` → `usuarios`, `refresh_tokens`

> **Nota:** El sistema cuenta con **dos portales de login** separados para mayor seguridad.
> - **ID Ciudadano** (`/auth/login`) — Acceso público para ciudadanos
> - **Login Interno** (`/auth/admin/login`) — Acceso restringido para gestores y administradores (URL separada)
>
> Ambos portales aceptan login por **email o CUIL** a través del campo `identificador`.

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/auth/login` | 🔓 | Login ciudadano (portal ID Ciudadano) — solo `tipo=CIUDADANO` |
| `POST` | `/auth/admin/login` | 🔓 | Login interno (portal Admin) — solo `tipo=INTERNO` |
| `POST` | `/auth/register` | 🔓 | Registro de nuevo ciudadano |
| `POST` | `/auth/forgot-password`| 🔓 | Solicitar link de recuperación de contraseña |
| `POST` | `/auth/reset-password` | 🔓 | Restablecer contraseña con token enviado por email |
| `POST` | `/auth/refresh` | 🔓 | Renovar access token con refresh token |
| `POST` | `/auth/logout` | 🔑 | Revocar refresh token activo |
| `POST` | `/auth/logout-all` | 🔑 | Revocar todos los refresh tokens del usuario |

### POST `/auth/login` — Portal ID Ciudadano
```json
// Request — el campo "identificador" acepta email O CUIL
{
  "identificador": "mgarcia@email.com",  // o "27-34567890-1"
  "password": "contraseña"
}

// Response 200
{
  "access_token": "eyJhbGci...",
  "refresh_token": "eyJhbGci...",
  "expires_in": 3600,
  "portal": "CIUDADANO",
  "usuario": {
    "id": 1,
    "nombre": "María",
    "apellido": "García",
    "email": "mgarcia@email.com",
    "rol": "CIUDADANO"
  }
}

// Errores
// 401 — Credenciales inválidas
// 403 — Usuario inactivo
// 403 — FORBIDDEN: El usuario no es ciudadano (tipo ≠ CIUDADANO)
```

### POST `/auth/admin/login` — Portal Interno (Admin/Gestor)
```json
// Request — el campo "identificador" acepta email O CUIL
{
  "identificador": "lmartinez@rdam.gob.ar",  // o "27-12345678-9"
  "password": "contraseña"
}

// Response 200
{
  "access_token": "eyJhbGci...",
  "refresh_token": "eyJhbGci...",
  "expires_in": 3600,
  "portal": "ADMIN",
  "usuario": {
    "id": 5,
    "nombre": "Laura",
    "apellido": "Martínez",
    "email": "lmartinez@rdam.gob.ar",
    "rol": "GESTOR"
  }
}

// Errores
// 401 — Credenciales inválidas
// 403 — Usuario inactivo
// 403 — FORBIDDEN: El usuario no es interno (tipo ≠ INTERNO)
```

---

## 2. Usuarios `/usuarios` → `usuarios`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `GET` | `/usuarios` | ⚙️ | Listar todos los usuarios internos |
| `GET` | `/usuarios/:id` | 🔑 | Ver perfil de usuario (propio o admin) |
| `POST` | `/usuarios` | ⚙️ | Crear nuevo usuario interno |
| `PATCH` | `/usuarios/:id` | ⚙️ | Editar datos de usuario |
| `PATCH` | `/usuarios/:id/rol` | ⚙️ | Cambiar rol de usuario interno |
| `PATCH` | `/usuarios/:id/estado` | ⚙️ | Activar / desactivar usuario |
| `DELETE` | `/usuarios/:id` | ⚙️ | Eliminar usuario (soft delete) |
| `GET` | `/usuarios/me` | 🔑 | Ver perfil del usuario autenticado |
| `PATCH` | `/usuarios/me` | 🔑 | Actualizar datos propios |
| `PATCH` | `/usuarios/me/password` | 🔑 | Cambiar contraseña propia |

### GET `/usuarios` — Query params
```
?rol=GESTOR
?activo=true
?search=laura          (busca en nombre, apellido, email)
?page=1&limit=20
?sort=created_at&order=desc
```

### POST `/usuarios`
```json
// Request
{
  "nombre": "Laura",
  "apellido": "Martínez",
  "email": "lmartinez@rdam.gob.ar",
  "password": "TempPass123!",
  "dni_cuil": "27-12345678-9",
  "telefono": "011-4444-5555",
  "rol": "GESTOR"
}

// Response 201
{
  "id": 5,
  "numero": null,
  "email": "lmartinez@rdam.gob.ar",
  "rol": "GESTOR",
  "activo": true,
  "created_at": "2026-02-18T15:00:00Z"
}
```

---

## 3. Solicitudes `/solicitudes` → `solicitudes`, `historial_estados`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `GET` | `/solicitudes` | 🏛️ | Listar todas las solicitudes (bandeja interna) |
| `GET` | `/solicitudes/mis` | 👤 | Listar solicitudes del ciudadano autenticado |
| `GET` | `/solicitudes/:id` | 🔑 | Ver detalle de una solicitud |
| `POST` | `/solicitudes` | 👤 | Crear nueva solicitud |
| `PATCH` | `/solicitudes/:id/tomar` | 🏛️ | Tomar solicitud para revisión |
| `PATCH` | `/solicitudes/:id/aprobar` | 🏛️ | Aprobar solicitud |
| `PATCH` | `/solicitudes/:id/rechazar` | 🏛️ | Rechazar solicitud con motivo |
| `GET` | `/solicitudes/:id/historial` | 🔑 | Ver historial de estados |

### GET `/solicitudes` — Query params (bandeja interna)
```
?estado=PENDIENTE_REVISION
?estado=EN_REVISION,APROBADA    (múltiples valores)
?tipo_cert=LIBRE_DEUDA
?urgencia=URGENTE
?ciudadano_cuil=27-34567890-1
?fecha_desde=2026-01-01
?fecha_hasta=2026-02-28
?revisor_id=3
?search=García                  (busca en nombre del ciudadano)
?page=1&limit=20
?sort=created_at&order=asc
```

### POST `/solicitudes`
```json
// Request
{
  "tipo_cert": "LIBRE_DEUDA",
  "urgencia": "NORMAL",
  "observaciones": "Necesito el certificado para trámite bancario"
}

// Response 201
{
  "id": 42,
  "numero": "SOL-2026-042",
  "estado": "PENDIENTE_REVISION",
  "tipo_cert": "LIBRE_DEUDA",
  "arancel": 1500.00,
  "created_at": "2026-02-18T15:00:00Z"
}
```

### PATCH `/solicitudes/:id/rechazar`
```json
// Request
{
  "motivo_rechazo": "La documentación adjunta está incompleta. Falta el comprobante de domicilio actualizado.",
  "comentario": "Se notificó al ciudadano por email."
}

// Response 200
{
  "id": 42,
  "estado": "RECHAZADA",
  "motivo_rechazo": "La documentación adjunta está incompleta...",
  "updated_at": "2026-02-18T16:00:00Z"
}
```

---

## 4. Pagos `/pagos` → `pagos`, `solicitudes`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/pagos/iniciar` | 👤 | Crear intención de pago en PlusPagos |
| `GET` | `/pagos/:id` | 🔑 | Ver estado de un pago |
| `GET` | `/pagos/solicitud/:solicitud_id` | 🔑 | Ver pago de una solicitud |
| `POST` | `/pagos/webhook` | 🔓 | Recibir confirmación de PlusPagos (HMAC validado) |

### POST `/pagos/iniciar`
```json
// Request
{
  "solicitud_id": 42
}

// Response 200
{
  "pago_id": 15,
  "checkout_url": "https://checkout.pluspagos.com/session/abc123",
  "monto": 1500.00,
  "moneda": "ARS",
  "expires_at": "2026-02-18T16:00:00Z"
}
```

### POST `/pagos/webhook` (PlusPagos → RDAM)
```json
// Headers requeridos
// X-PlusPagos-Signature: sha256=<hmac>

// Payload
{
  "referencia": "SOL-2026-042",
  "estado": "APROBADO",
  "monto": 1500.00,
  "transaccion_id": "PP-TXN-789456",
  "timestamp": "2026-02-18T15:30:00Z"
}

// Response 200
{ "ok": true }
```

---

## 5. Certificados `/certificados` → `certificados`, `solicitudes`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/certificados/emitir` | 🏛️ | Generar y emitir certificado PDF |
| `GET` | `/certificados/:id` | 🔑 | Ver metadatos del certificado |
| `GET` | `/certificados/:id/descargar` | 🔑 | Descargar PDF (URL presignada S3) |
| `GET` | `/certificados/solicitud/:solicitud_id` | 🔑 | Ver certificado de una solicitud |
| `GET` | `/certificados/:id/verificar` | 🔓 | Verificar autenticidad del certificado |

### POST `/certificados/emitir`
```json
// Request
{
  "solicitud_id": 42,
  "comentario": "Certificado emitido conforme a la documentación presentada."
}

// Response 201
{
  "id": 8,
  "solicitud_id": 42,
  "archivo_url": "https://storage.rdam.gob.ar/certs/SOL-2026-042.pdf",
  "fecha_vencimiento": "2026-05-19",
  "created_at": "2026-02-18T17:00:00Z"
}
```

### GET `/certificados/:id/verificar` (público)
```json
// Response 200 — Certificado válido
{
  "valido": true,
  "numero_solicitud": "SOL-2026-042",
  "tipo": "LIBRE_DEUDA",
  "ciudadano": "María García",
  "emitido_por": "Municipalidad de RDAM",
  "fecha_emision": "2026-02-18",
  "fecha_vencimiento": "2026-05-19",
  "firma_verificada": true
}

// Response 200 — Certificado expirado o inválido
{
  "valido": false,
  "razon": "EXPIRADO"
}
```

---

## 6. Reportes `/reportes` → `solicitudes`, `pagos`, `certificados`, `usuarios`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `GET` | `/reportes/resumen` | 📊 | Métricas generales del sistema |
| `GET` | `/reportes/solicitudes` | 📊 | Solicitudes por estado, tipo y período |
| `GET` | `/reportes/tiempos` | 📊 | Tiempos promedio de resolución |
| `GET` | `/reportes/gestores` | 📊 | Productividad por gestor |
| `GET` | `/reportes/exportar` | 📊 | Exportar datos en CSV |

### GET `/reportes/resumen`
```json
// Response 200
{
  "periodo": "2026-02",
  "solicitudes": {
    "total": 156,
    "pendiente_revision": 12,
    "en_revision": 8,
    "aprobadas": 45,
    "rechazadas": 11,
    "pendiente_pago": 7,
    "pagadas": 3,
    "emitidas": 70
  },
  "tiempo_promedio_resolucion_hs": 18.4,
  "ingresos_mes": 234000.00
}
```

---

## 7. Códigos de Error Estándar

| Código HTTP | Código interno | Descripción |
|-------------|---------------|-------------|
| `400` | `VALIDATION_ERROR` | Datos de entrada inválidos |
| `400` | `INVALID_TRANSITION` | Transición de estado no permitida |
| `401` | `UNAUTHORIZED` | Token ausente o inválido |
| `401` | `TOKEN_EXPIRED` | Access token expirado |
| `403` | `FORBIDDEN` | Sin permisos para esta acción |
| `403` | `USER_INACTIVE` | Usuario desactivado |
| `404` | `NOT_FOUND` | Recurso no encontrado |
| `409` | `CONFLICT` | Conflicto (ej: email duplicado) |
| `422` | `BUSINESS_RULE` | Regla de negocio violada |
| `429` | `RATE_LIMITED` | Demasiadas solicitudes |
| `500` | `INTERNAL_ERROR` | Error interno del servidor |
| `503` | `SERVICE_UNAVAILABLE` | Servicio temporalmente no disponible |

### Formato de error estándar
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "El campo 'email' no tiene un formato válido.",
    "field": "email",
    "timestamp": "2026-02-18T15:00:00Z",
    "request_id": "req_abc123"
  }
}
```

---

## 8. Rate Limiting

| Endpoint | Límite | Ventana |
|----------|--------|---------|
| `POST /auth/login` | 10 req | 15 min por IP |
| `POST /pagos/webhook` | 100 req | 1 min por IP |
| Endpoints públicos | 60 req | 1 min por IP |
| Endpoints autenticados | 300 req | 1 min por usuario |

---

## 9. Resumen de Endpoints por Rol

| Rol | Portal de Login | Endpoints disponibles |
|-----|----------------|----------------------|
| **Ciudadano** | ID Ciudadano (`/auth/login`) | `/auth/login`, `/auth/register`, `/usuarios/me`, `/solicitudes/mis`, `POST /solicitudes`, `GET /solicitudes/:id`, `POST /pagos/iniciar`, `GET /certificados/:id/descargar` |
| **Gestor** | Login Interno (`/auth/admin/login`) | `/auth/admin/login`, `/usuarios/me` + `GET /solicitudes`, `PATCH /solicitudes/:id/tomar`, `PATCH /solicitudes/:id/aprobar`, `PATCH /solicitudes/:id/rechazar`, `POST /certificados/emitir` |
| **Admin** | Login Interno (`/auth/admin/login`) | Todo lo del Gestor + `GET/POST/PATCH/DELETE /usuarios`, `GET /reportes/*` |

---

## 10. Mapeo Endpoint ↔ Entidad/Tabla

Tabla de referencia cruzada entre cada grupo de endpoints y las tablas del DDL que operan.

| Grupo de Endpoints | Tabla Principal | Tablas Relacionadas | Operaciones |
|---|---|---|---|
| `/auth` | `usuarios` | `refresh_tokens` | Login dual (ciudadano + admin), registro, refresh/revoke tokens |
| `/usuarios` | `usuarios` | — | CRUD usuarios internos, perfil propio |
| `/solicitudes` | `solicitudes` | `historial_estados`, `usuarios` | CRUD solicitudes, transiciones de estado, historial |
| `/pagos` | `pagos` | `solicitudes` | Iniciar pago, webhook PlusPagos, consultar estado |
| `/certificados` | `certificados` | `solicitudes`, `usuarios` | Emitir PDF, descargar, verificar autenticidad |
| `/reportes` | `solicitudes` | `pagos`, `certificados`, `usuarios` | Métricas consolidadas, exportación CSV |

### Cobertura de Tablas DDL

| Tabla DDL | Endpoints que la operan | Tipo de acceso |
|---|---|---|
| `usuarios` | `/auth/*`, `/usuarios/*` | R/W |
| `solicitudes` | `/solicitudes/*`, `/pagos/*`, `/certificados/*`, `/reportes/*` | R/W |
| `historial_estados` | `/solicitudes/:id/historial` (lectura), trigger automático (escritura) | R (API) / W (trigger) |
| `pagos` | `/pagos/*`, `/reportes/*` | R/W |
| `certificados` | `/certificados/*`, `/reportes/*` | R/W |
| `refresh_tokens` | `/auth/refresh`, `/auth/logout`, `/auth/logout-all` | R/W |
