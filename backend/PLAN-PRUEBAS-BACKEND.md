# PLAN DE PRUEBAS BACKEND — RDAM
## i2T Software Factory · Campus de Verano 2026
### Alumno: Serovich Emilio — DNI 43.770.166 — 3° T.S. "A"

---

## 1. Alcance

Este documento describe el plan de pruebas para el backend del sistema RDAM, cubriendo todos los módulos de la API REST: Autenticación, Usuarios, Solicitudes, Pagos, Certificados y Reportes.

---

## 2. Objetivos

- Validar el correcto funcionamiento de la lógica de negocio de cada módulo.
- Verificar el control de acceso basado en roles (CIUDADANO, GESTOR, ADMIN).
- Comprobar la máquina de estados de solicitudes.
- Asegurar que los errores retornan el formato estándar definido en API-ENDPOINTS.md.

---

## 3. Tipos de Prueba

| Tipo | Herramienta | Descripción |
|------|------------|-------------|
| **Unitario** | JUnit 5 + Mockito | Prueba lógica de servicios en aislamiento |
| **Integración** | MockMvc + H2 | Prueba controllers con Spring context cargado |
| **Manual / E2E** | Postman | Flujo completo ejecutado contra servidor local |

---

## 4. Pruebas Unitarias

### 4.1 Módulo Auth — `AuthServiceTest`

| ID | Caso de prueba | Resultado esperado |
|----|---------------|-------------------|
| UT-A01 | Login ciudadano con email válido | `200 OK`, access_token presente, portal=CIUDADANO |
| UT-A02 | Login ciudadano con CUIL válido | `200 OK`, portal=CIUDADANO |
| UT-A03 | Login ciudadano con contraseña incorrecta | `401 UNAUTHORIZED` |
| UT-A04 | Login ciudadano usando portal admin (`/auth/admin/login`) | `403 FORBIDDEN` |
| UT-A05 | Login interno con tipo=CIUDADANO en portal admin | `403 FORBIDDEN` |
| UT-A06 | Registro con email duplicado | `409 CONFLICT` |
| UT-A07 | Refresh token válido | Nuevo access_token + rotación de refresh token |
| UT-A08 | Refresh token revocado | `401 UNAUTHORIZED` |

### 4.2 Módulo Solicitudes — `SolicitudServiceTest`

| ID | Caso de prueba | Resultado esperado |
|----|---------------|-------------------|
| UT-S01 | Crear solicitud como ciudadano | Estado inicial `PENDIENTE_REVISION`, arancel=1500 |
| UT-S02 | Tomar solicitud (PENDIENTE → EN_REVISION) | Estado `EN_REVISION`, revisor asignado |
| UT-S03 | Transición inválida (RECHAZADA → EN_REVISION) | `400 INVALID_TRANSITION` |
| UT-S04 | Aprobar solicitud (EN_REVISION → PENDIENTE_PAGO) | Estado `PENDIENTE_PAGO` (paso intermedio APROBADA automático) |
| UT-S05 | Rechazar sin motivo | `400 VALIDATION_ERROR` |
| UT-S06 | Rechazar con motivo válido | Estado `RECHAZADA`, motivoRechazo registrado |
| UT-S07 | Ciudadano accede a solicitud ajena | `403 FORBIDDEN` |
| UT-S08 | Solicitud inexistente | `404 NOT_FOUND` |

### 4.3 Módulo Pagos — `PagoServiceTest`

| ID | Caso de prueba | Resultado esperado |
|----|---------------|-------------------|
| UT-P01 | Iniciar pago en solicitud PENDIENTE_PAGO | Pago creado con checkout_url |
| UT-P02 | Iniciar pago en solicitud no PENDIENTE_PAGO | `422 BUSINESS_RULE` |
| UT-P03 | Webhook APROBADO | Solicitud pasa a estado PAGADA |
| UT-P04 | Webhook RECHAZADO | Pago pasa a estado RECHAZADO, solicitud sin cambio |
| UT-P05 | Webhook con referencia inexistente | `404 NOT_FOUND` |

---

## 5. Pruebas de Integración (MockMvc)

### 5.1 Endpoints de Auth

| ID | Endpoint | Escenario | Estado esperado |
|----|----------|-----------|----------------|
| IT-A01 | `POST /auth/login` | Body válido | 200 |
| IT-A02 | `POST /auth/login` | Body vacío | 400 |
| IT-A03 | `POST /auth/admin/login` | Ciudadano intenta acceso | 403 |
| IT-A04 | `POST /auth/register` | Email ya existente | 409 |
| IT-A05 | `POST /auth/register` | Password < 8 chars | 400 |
| IT-A06 | `POST /auth/refresh` | Token válido | 200 |

### 5.2 Control de Acceso por Rol

| ID | Endpoint | Rol usado | Estado esperado |
|----|----------|-----------|----------------|
| IT-R01 | `GET /usuarios` | CIUDADANO | 403 |
| IT-R02 | `GET /usuarios` | ADMIN | 200 |
| IT-R03 | `GET /solicitudes` | CIUDADANO | 403 |
| IT-R04 | `GET /solicitudes` | GESTOR | 200 |
| IT-R05 | `POST /certificados/emitir` | CIUDADANO | 403 |
| IT-R06 | `GET /reportes/resumen` | GESTOR | 403 |
| IT-R07 | `GET /reportes/resumen` | ADMIN | 200 |
| IT-R08 | `GET /certificados/:id/verificar` | Sin token | 200 (público) |

### 5.3 Flujo Completo de Solicitud

| ID | Paso | Actor | Acción | Estado esperado |
|----|------|-------|--------|----------------|
| IT-F01 | 1 | Ciudadano | `POST /solicitudes` | PENDIENTE_REVISION |
| IT-F02 | 2 | Gestor | `PATCH /solicitudes/:id/tomar` | EN_REVISION |
| IT-F03 | 3 | Gestor | `PATCH /solicitudes/:id/aprobar` | PENDIENTE_PAGO |
| IT-F04 | 4 | Sistema | `POST /pagos/iniciar` | Pago creado |
| IT-F05 | 5 | PlusPagos | `POST /pagos/webhook` (APROBADO) | PAGADA |
| IT-F06 | 6 | Gestor | `POST /certificados/emitir` | EMITIDA, PDF generado |
| IT-F07 | 7 | Ciudadano | `GET /certificados/:id/descargar` | 200, PDF binario |
| IT-F08 | 8 | Público | `GET /certificados/:id/verificar` | valido: true |

---

## 6. Pruebas de Errores Estándar

| ID | Escenario | Código HTTP | Código interno |
|----|-----------|-------------|----------------|
| ET-01 | Campo obligatorio ausente | 400 | VALIDATION_ERROR |
| ET-02 | Transición de estado inválida | 400 | INVALID_TRANSITION |
| ET-03 | Token JWT ausente o inválido | 401 | UNAUTHORIZED |
| ET-04 | Token JWT expirado | 401 | TOKEN_EXPIRED |
| ET-05 | Rol insuficiente | 403 | FORBIDDEN |
| ET-06 | Usuario desactivado | 403 | USER_INACTIVE |
| ET-07 | Recurso no encontrado | 404 | NOT_FOUND |
| ET-08 | Email duplicado en registro | 409 | CONFLICT |
| ET-09 | Regla de negocio violada | 422 | BUSINESS_RULE |
| ET-10 | Error interno del servidor | 500 | INTERNAL_ERROR |

---

## 7. Criterios de Aceptación

| Criterio | Umbral mínimo |
|----------|--------------|
| Tests unitarios pasando | 100% |
| Cobertura de líneas (servicios) | ≥ 80% |
| Endpoints documentados en Postman | 100% |
| Flujo completo E2E sin errores | ✅ |
| Errores con formato estándar | 100% |
| JWT doble portal funcionando | ✅ |

---

## 8. Datos de Prueba

| Usuario | Email | Password | Rol |
|---------|-------|----------|-----|
| Ciudadano | `mgarcia@email.com` | `Password1!` | CIUDADANO |
| Gestor | `lmartinez@rdam.gob.ar` | `Password1!` | GESTOR |
| Admin | `admin@rdam.gob.ar` | `Password1!` | ADMIN |

> Generados automáticamente por `DataInitializer` al iniciar con perfil `dev`.

---

## 9. Cómo Ejecutar

### Tests automáticos (JUnit)
```bash
cd backend
mvn test
```

### Backend en modo dev
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Postman
1. Importar `RDAM-API.postman_collection.json`
2. Importar `RDAM-Environment.postman_environment.json`
3. Seleccionar environment "RDAM — Local Dev"
4. Ejecutar carpeta `1. Auth > Login Ciudadano` para obtener el token
5. Ejecutar las demás carpetas en orden

---

*Documento generado para la Fase 2 del proyecto RDAM — Campus de Verano 2026*
