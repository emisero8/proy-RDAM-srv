# PLAN DE PRUEBAS UI — RDAM Frontend
## i2T Software Factory · Campus de Verano 2026
### Alumno: Serovich Emilio — DNI 43.770.166 — 3° T.S. "A"

---

## 1. Alcance

Este documento describe el plan de pruebas para el frontend del sistema RDAM, cubriendo todas las pantallas y flujos de la aplicación React que consume la API REST del backend.

---

## 2. Objetivos

- Validar la correcta renderización de todas las pantallas según el rol del usuario.
- Verificar que las rutas protegidas impiden el acceso a usuarios no autorizados.
- Comprobar la integración frontend ↔ backend en los flujos completos.
- Asegurar que los formularios validan datos antes del envío.
- Verificar el comportamiento responsive en distintos anchos de pantalla.

---

## 3. Tipos de Prueba

| Tipo | Herramienta | Descripción |
|------|------------|-------------|
| **Funcional / E2E** | Manual (Browser) | Navegación completa de flujos por rol |
| **Visual** | Inspección manual | Verificar diseño, contraste, tipografía y animaciones |
| **Responsive** | DevTools/Resize | Verificar adaptación en mobile (480px) y tablet (768px) |
| **Integración** | Postman + Browser | Flujo completo backend + frontend conectados |

---

## 4. Casos de Prueba — Autenticación

| ID | Caso | Pasos | Resultado esperado |
|----|------|-------|-------------------|
| UI-A01 | Login ciudadano exitoso | Ir a `/login`, ingresar email y password válidos | Redirige a `/solicitudes/mis` |
| UI-A02 | Login ciudadano con error | Ingresar password incorrecta | Muestra mensaje de error "Credenciales inválidas" |
| UI-A03 | Login admin exitoso | Ir a `/admin/login`, ingresar credenciales gestor | Redirige a `/bandeja` |
| UI-A04 | Login admin como ciudadano | Usar credenciales de ciudadano en `/admin/login` | Muestra error 403 "FORBIDDEN" |
| UI-A05 | Registro exitoso | Ir a `/registro`, completar formulario | Muestra mensaje de éxito y redirige a `/login` |
| UI-A06 | Registro con email duplicado | Intentar registrar con email ya existente | Muestra error "CONFLICT" |
| UI-A07 | Logout | Click en botón "Salir" | Redirige a `/login`, tokens eliminados |
| UI-A08 | Ruta protegida sin login | Navegar a `/solicitudes/mis` sin sesión | Redirige a `/login` |

---

## 5. Casos de Prueba — Ciudadano

| ID | Caso | Pasos | Resultado esperado |
|----|------|-------|-------------------|
| UI-C01 | Ver mis solicitudes | Login como ciudadano → `/solicitudes/mis` | Lista de solicitudes del ciudadano |
| UI-C02 | Empty state sin solicitudes | Login con ciudadano nuevo sin solicitudes | Muestra "No tenés solicitudes todavía" |
| UI-C03 | Crear solicitud | Click "Nueva Solicitud" → completar form → enviar | Solicitud creada, aparece en lista con estado PENDIENTE_REVISION |
| UI-C04 | Ver detalle de solicitud | Click en una solicitud de la lista | Muestra detalle con número, estado, arancel, fecha |
| UI-C05 | Pagar arancel (PENDIENTE_PAGO) | En detalle de solicitud PENDIENTE_PAGO, click "Pagar" | Abre checkout URL en nueva pestaña |
| UI-C06 | Ciudadano no ve bandeja | Intentar navegar a `/bandeja` | Redirige a `/` (ruta no autorizada) |
| UI-C07 | Ciudadano no ve usuarios | Intentar navegar a `/usuarios` | Redirige a `/` |

---

## 6. Casos de Prueba — Gestor

| ID | Caso | Pasos | Resultado esperado |
|----|------|-------|-------------------|
| UI-G01 | Ver bandeja | Login como gestor → `/bandeja` | Lista de solicitudes con filtro de estado |
| UI-G02 | Filtrar por estado | Seleccionar estado en dropdown | Muestra solo solicitudes con ese estado |
| UI-G03 | Tomar solicitud | Click en solicitud PENDIENTE → "Tomar" | Estado cambia a EN_REVISION |
| UI-G04 | Aprobar solicitud | En solicitud EN_REVISION → "Aprobar" | Estado cambia a PENDIENTE_PAGO |
| UI-G05 | Rechazar solicitud | Click "Rechazar" → completar motivo → confirmar | Modal se abre, estado cambia a RECHAZADA |
| UI-G06 | Rechazar sin motivo | Click "Confirmar Rechazo" con campo vacío | Botón deshabilitado (campo requerido) |
| UI-G07 | Emitir certificado | En solicitud PAGADA → "Emitir Certificado" | Estado cambia a EMITIDA |
| UI-G08 | Historial de estados | En detalle de solicitud con transiciones | Timeline muestra todas las transiciones |

---

## 7. Casos de Prueba — Admin

| ID | Caso | Pasos | Resultado esperado |
|----|------|-------|-------------------|
| UI-AD01 | Ver panel de usuarios | Login como admin → `/usuarios` | Tabla con usuarios internos |
| UI-AD02 | Crear usuario interno | Click "Nuevo Usuario" → completar modal → crear | Modal se cierra, usuario aparece en tabla |
| UI-AD03 | Desactivar usuario | Click "Desactivar" en un usuario activo | Badge cambia a "Inactivo" |
| UI-AD04 | Activar usuario | Click "Activar" en un usuario inactivo | Badge cambia a "Activo" |
| UI-AD05 | Ver reportes | Navegar a `/reportes` | Dashboard con stats y tabla de desglose |
| UI-AD06 | Admin ve bandeja | Navegar a `/bandeja` | Admin tiene acceso a la bandeja interna |

---

## 8. Casos de Prueba — Navegación y UX

| ID | Caso | Resultado esperado |
|----|------|-------------------|
| UI-N01 | Navbar muestra links según rol | Ciudadano: Mis Solicitudes, Nueva. Gestor: Bandeja. Admin: Bandeja, Usuarios, Reportes |
| UI-N02 | Badge de rol en navbar | Muestra badge con el rol del usuario (CIUDADANO / GESTOR / ADMIN) |
| UI-N03 | Volver desde detalle | Botón "← Volver" funciona correctamente |
| UI-N04 | Loading spinners | Se muestran spinners durante las cargas de datos |
| UI-N05 | Mensajes de error | Errores de API se muestran como toast de error |

---

## 9. Casos de Prueba — Responsive

| ID | Ancho | Elementos a verificar | Resultado esperado |
|----|-------|----------------------|-------------------|
| UI-R01 | ≤ 480px | Cards, formularios, auth | Layout de una columna, inputs full-width |
| UI-R02 | 481–768px | Stats grid, filtros, tabla | Grid de 2 columnas, tabla con scroll horizontal |
| UI-R03 | ≥ 769px | Layout completo | Navegación horizontal, grid multi-columna |

---

## 10. Flujo E2E Completo (Frontend + Backend)

| Paso | Actor | Acción en UI | Endpoint backend | Resultado |
|------|-------|-------------|-----------------|-----------|
| 1 | Ciudadano | Login en `/login` | `POST /auth/login` | Token guardado, redirige a `/solicitudes/mis` |
| 2 | Ciudadano | Crear solicitud | `POST /solicitudes` | Solicitud en estado PENDIENTE_REVISION |
| 3 | Gestor | Login en `/admin/login` | `POST /auth/admin/login` | Token guardado, redirige a `/bandeja` |
| 4 | Gestor | Tomar solicitud | `PATCH /solicitudes/:id/tomar` | Estado → EN_REVISION |
| 5 | Gestor | Aprobar solicitud | `PATCH /solicitudes/:id/aprobar` | Estado → PENDIENTE_PAGO |
| 6 | Ciudadano | Pagar arancel | `POST /pagos/iniciar` | Pago creado, checkout_url |
| 7 | Sistema | Webhook de pago | `POST /pagos/webhook` | Estado → PAGADA |
| 8 | Gestor | Emitir certificado | `POST /certificados/emitir` | Estado → EMITIDA, PDF generado |

---

## 11. Datos de Prueba

| Usuario | Portal | Email | Password | Rol |
|---------|--------|-------|----------|-----|
| Ciudadano | `/login` | `mgarcia@email.com` | `Password1!` | CIUDADANO |
| Gestor | `/admin/login` | `lmartinez@rdam.gob.ar` | `Password1!` | GESTOR |
| Admin | `/admin/login` | `admin@rdam.gob.ar` | `Password1!` | ADMIN |

> Generados automáticamente por `DataInitializer` al iniciar el backend con perfil `dev`.

---

## 12. Cómo Ejecutar

### 1. Levantar backend (perfil dev con H2)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Levantar frontend
```bash
cd frontend
npm run dev
```

### 3. Abrir navegador
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/v1`
- H2 Console: `http://localhost:8080/v1/h2-console`

---

*Documento generado para la Fase 3 del proyecto RDAM — Campus de Verano 2026*
