# ESPECIFICACIÓN FUNCIONAL: RDAM
## i2T Software Factory | Campus de Verano 2026

### 1. Resumen Ejecutivo

RDAM (Registro de Deudores Alimentarios Morosos) es una plataforma web para la gestión integral de solicitudes y emisión de certificados de libre deuda. Permite a los ciudadanos solicitar certificados en línea, pagar aranceles a través de la pasarela PlusPagos, y recibir documentos PDF por email. El personal interno revisa, aprueba y emite los certificados digitalmente, eliminando por completo el trámite presencial.

### 2. Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        FRONTEND                             │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ Portal   │  │ Panel Interno│  │ Panel Administración  │  │
│  │Ciudadano │  │ (Gestor)     │  │ (Admin)               │  │
│  └────┬─────┘  └──────┬───────┘  └──────────┬────────────┘  │
│       └───────────────┼──────────────────────┘              │
├───────────────────────┼─────────────────────────────────────┤
│                   API REST                                  │
│  /solicitudes  /certificados  /pagos  /usuarios  /auth      │
├───────────────────────┼─────────────────────────────────────┤
│                    BACKEND                                  │
│  ┌─────────┐  ┌──────┴─────┐  ┌──────────┐  ┌───────────┐  │
│  │Solicitud│  │Certificado │  │ Notific. │  │  Auth     │  │
│  │Service  │  │Service     │  │ Service  │  │  Service  │  │
│  └────┬────┘  └──────┬─────┘  └────┬─────┘  └─────┬─────┘  │
├───────┼──────────────┼──────────────┼──────────────┼────────┤
│       │         ┌────┴────┐   ┌────┴────┐                   │
│       │         │PDF Gen  │   │  SMTP   │                   │
│       │         └─────────┘   └─────────┘                   │
├───────┼─────────────────────────────────────────────────────┤
│  ┌────┴────┐              ┌─────────────┐                   │
│  │   DB    │              │ PlusPagos   │  (Pasarela ext.)  │
│  │PostgreSQL│             │   API       │                   │
│  └─────────┘              └─────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

| Rol | Descripción | Portal de Login | Capacidades |
|-----|-------------|-----------------|-------------|
| **Ciudadano** | Usuario público que solicita certificados | ID Ciudadano (`/auth/login`) | Crear solicitud, ver sus solicitudes, pagar arancel, descargar certificado emitido |
| **Gestor** | Empleado que revisa y emite certificados | Login Interno (`/auth/admin/login`) | Ver todas las solicitudes, aprobar/rechazar, emitir certificado PDF |
| **Administrador** | Gestiona accesos al sistema | Login Interno (`/auth/admin/login`) | Todo lo del Gestor + reportes y métricas + CRUD de usuarios internos |

### 4. Historias de Usuario

| ID | Historia | Criterios de Aceptación |
|----|----------|------------------------|
| HU-01 | Como ciudadano, quiero crear una solicitud de certificado para iniciar mi trámite | Formulario con datos personales y documentación. Estado inicial: PENDIENTE REVISIÓN |
| HU-02 | Como ciudadano, quiero ver el estado de mis solicitudes para dar seguimiento | Lista filtrable con estado, fecha, acciones disponibles |
| HU-03 | Como ciudadano, quiero pagar el arancel tras la aprobación para avanzar el trámite | Redirección a PlusPagos, confirmación automática, cambio a estado PAGADA |
| HU-04 | Como ciudadano, quiero descargar mi certificado emitido | Botón de descarga visible solo en estado EMITIDA |
| HU-05 | Como gestor, quiero ver todas las solicitudes pendientes para priorizarlas | Bandeja filtrable por estado, fecha, solicitante |
| HU-06 | Como gestor, quiero aprobar una solicitud válida | Modal de confirmación, campo de comentario, notificación al ciudadano |
| HU-07 | Como gestor, quiero rechazar una solicitud con observaciones | Modal con motivo obligatorio, observaciones detalladas, notificación |
| HU-08 | Como gestor, quiero emitir un certificado tras el pago | Generación automática de PDF, envío por email, descarga directa |
| HU-09 | Como admin, quiero gestionar usuarios internos | CRUD completo: crear, editar, desactivar, asignar roles |
| HU-10 | Como ciudadano, quiero iniciar sesión con mi email o CUIL y contraseña para acceder al portal ID Ciudadano | Login por email o CUIL, validación de tipo CIUDADANO, redirección al dashboard ciudadano |
| HU-11 | Como gestor/admin, quiero iniciar sesión desde una URL separada (/admin/login) para mayor seguridad | Login por email o CUIL, validación de tipo INTERNO, acceso solo desde URL aparte, botón discreto en login ciudadano |

### 5. Modelo de Datos

```sql
CREATE TABLE usuarios (
  id            SERIAL PRIMARY KEY,
  nombre        VARCHAR(100) NOT NULL,
  email         VARCHAR(150) UNIQUE NOT NULL,
  dni_cuil      VARCHAR(20) UNIQUE,
  telefono      VARCHAR(30),
  domicilio     TEXT,
  tipo          VARCHAR(10) CHECK (tipo IN ('CIUDADANO','INTERNO')),
  rol           VARCHAR(15) CHECK (rol IN ('CIUDADANO','GESTOR','ADMIN')),
  activo        BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE solicitudes (
  id            SERIAL PRIMARY KEY,
  numero        VARCHAR(20) UNIQUE NOT NULL,       -- SOL-YYYY-NNN
  ciudadano_id  INT REFERENCES usuarios(id),
  tipo_cert     VARCHAR(50) DEFAULT 'LIBRE_DEUDA',
  urgencia      VARCHAR(10) DEFAULT 'NORMAL',
  estado        VARCHAR(20) DEFAULT 'PENDIENTE_REVISION',
  arancel       DECIMAL(10,2),
  observaciones TEXT,
  revisor_id    INT REFERENCES usuarios(id),
  created_at    TIMESTAMP DEFAULT NOW(),
  updated_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE historial_estados (
  id            SERIAL PRIMARY KEY,
  solicitud_id  INT REFERENCES solicitudes(id),
  estado_ant    VARCHAR(20),
  estado_nuevo  VARCHAR(20) NOT NULL,
  usuario_id    INT REFERENCES usuarios(id),
  comentario    TEXT,
  created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE pagos (
  id            SERIAL PRIMARY KEY,
  solicitud_id  INT REFERENCES solicitudes(id),
  monto         DECIMAL(10,2) NOT NULL,
  referencia    VARCHAR(50),           -- ID de PlusPagos
  estado        VARCHAR(15) DEFAULT 'PENDIENTE',
  created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE certificados (
  id            SERIAL PRIMARY KEY,
  solicitud_id  INT REFERENCES solicitudes(id),
  archivo_url   TEXT NOT NULL,
  emisor_id     INT REFERENCES usuarios(id),
  firma_digital TEXT,
  created_at    TIMESTAMP DEFAULT NOW()
);
```

### 6. Flujo de Estados

```
  ┌──────────────────┐
  │PENDIENTE REVISIÓN│ ← Estado inicial (al crear solicitud)
  └────────┬─────────┘
           │ Interno toma la solicitud
  ┌────────▼─────────┐
  │   EN REVISIÓN    │
  └────────┬─────────┘
           │
     ┌─────┴──────┐
     │            │
┌────▼────┐  ┌────▼─────┐
│APROBADA │  │RECHAZADA │ (fin del flujo)
└────┬────┘  └──────────┘
     │
┌────▼──────────┐
│PENDIENTE PAGO │
└────┬──────────┘
     │ Ciudadano paga via PlusPagos
┌────▼────┐
│ PAGADA  │
└────┬────┘
     │ Interno emite certificado
┌────▼────┐
│ EMITIDA │ (fin del flujo vigente)
└────┬────┘
     │ Sistema (via cron > 90 días)
┌────▼────┐
│EXPIRADA │
└─────────┘
```

| Estado Origen | Estado Destino | Actor | Acción |
|---------------|---------------|-------|--------|
| PENDIENTE REVISIÓN | EN REVISIÓN | Interno | Tomar solicitud |
| EN REVISIÓN | APROBADA | Interno | Aprobar |
| EN REVISIÓN | RECHAZADA | Interno | Rechazar con observaciones |
| APROBADA | PENDIENTE PAGO | Sistema | Automático tras aprobación |
| PENDIENTE PAGO | PAGADA | PlusPagos | Confirmación de pago |
| PAGADA | EMITIDA | Interno | Emitir certificado |
| EMITIDA | EXPIRADA | Sistema | Automático tras 90 días de validez |
| EXPIRADA | PENDIENTE REVISIÓN | Ciudadano | Renovar trámite (nueva solicitud) |

### 7. Integraciones

#### PlusPagos (Pasarela de Pago)
- **Tipo**: API REST externa
- **Operaciones**: Crear intención de pago, recibir webhook de confirmación
- **Autenticación**: API Key + Secret
- **Datos enviados**: monto, referencia (N° solicitud), descripción
- **Datos recibidos**: estado del pago, referencia de transacción
- **Seguridad**: Los datos de tarjeta nunca pasan por servidores RDAM

#### Servicio de Email (SMTP)
- **Tipo**: SMTP o servicio transaccional (SendGrid, AWS SES)
- **Operaciones**: Envío de notificaciones de cambio de estado, envío de certificado PDF adjunto

### 8. Requisitos No Funcionales

| Requisito | Detalle |
|-----------|---------|
| **Performance** | Tiempo de respuesta < 2s para operaciones CRUD. Generación de PDF < 5s |
| **Seguridad** | HTTPS obligatorio, autenticación JWT, roles con permisos granulares, encriptación de datos sensibles, login separado para ciudadanos (`/auth/login`) y personal interno (`/auth/admin/login`) por URL distinta |
| **Disponibilidad** | 99.5% uptime en horario laboral (L-V 8-18hs) |
| **Escalabilidad** | Soportar hasta 1000 solicitudes concurrentes |
| **Auditoría** | Log completo de todas las acciones con usuario, timestamp y detalle. Registro de portal de login utilizado |
| **Accesibilidad** | WCAG 2.1 nivel AA |
