# RDAM — Paquete de Producción
## Gestión de Solicitudes y Certificados de Libre Deuda
### i2T Software Factory | Campus de Verano 2026
**Alumno:** Serovich Emilio — DNI 43.770.166 — 3º T.S "A"

## Contenido

| Archivo | Descripción |
|---------|-------------|
| `poc-rdam.html` | POC navegable de alta fidelidad — abrir directamente en el navegador |
| `ALCANCE.md` | Documento de alcance del proyecto (módulos, historias, plan de entrega) |
| `ARQUITECTURA.html` | Diagrama de arquitectura visual — abrir en el navegador |
| `SPEC.md` | Especificación funcional completa (roles, datos, estados, integraciones) |
| `IMPLEMENTACION.md` | Guía de implementación con plan de sprints y decisiones técnicas |
| `README.md` | Este archivo — índice del paquete |

## Cómo Usar

### 1. Revisar el POC
Abrir `poc-rdam.html` en cualquier navegador moderno (Chrome, Firefox, Edge). El POC es autocontenido, solo requiere conexión a internet para cargar Google Fonts.

**Funcionalidades del POC:**
- **Cambio de rol** en la sidebar: Ciudadano / Interno / Admin
- **Navegación completa** entre todas las vistas
- **Modales interactivos** para aprobar, rechazar, emitir y pagar
- **Filtros funcionales** en tablas de solicitudes
- **Toasts de notificación** para feedback de acciones

### 2. Revisar la Especificación
Leer `SPEC.md` para entender el alcance funcional completo: modelo de datos, flujo de estados, integraciones, y requisitos no funcionales.

### 3. Planificar la Implementación
Seguir `IMPLEMENTACION.md` para el plan de sprints (5 sprints, ~9 semanas), decisiones técnicas, estrategia de testing y deployment.

## Resumen del Sistema

> **RDAM en una frase:** Todo digital, todo trazable y todo simple.

El flujo principal es: **Solicitar → Revisar → Aprobar → Pagar → Emitir → Descargar**

Tres perfiles de usuario: **Ciudadano** (solicita y paga), **Interno** (revisa y emite), **Administrador** (gestiona usuarios).
