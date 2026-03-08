# RDAM — Pendientes / Deuda Técnica

## Pendientes de Fase 0

Los siguientes items fueron identificados en la revisión de Fase 0
y se dejan apuntados para corregir en una futura iteración.

### Stack tecnológico desactualizado en documentos

| Documento | Dice | Debería decir |
|---|---|---|
| `ALCANCE-RDAM-Serovich_Emilio.html` (sección 3.1) | Backend: Node.js (NestJS) o .NET Core | Backend: Spring Boot (Java 17+) |
| `DIAG_ARQUITECTURA-RDAM-Serovich_Emilio.html` | Backend [API REST · Node.js / Express] | Backend [API REST · Spring Boot] |

### Nomenclatura de estados inconsistente en POC

| Lugar | Nombre usado | Nombre correcto (DDL) |
|---|---|---|
| POC filtro ciudadano | ESPERANDO PAGO | PENDIENTE_PAGO |
| POC flujo de estados alcance | PENDIENTE ASIGNAR | PENDIENTE_REVISION |
| POC flujo de estados alcance | ASIGNADA | EN_REVISION |
| POC flujo de estados alcance | CERT. CARGADO | APROBADA |

> **Nota:** Estos cambios no son urgentes. Se realizarán cuando se actualice
> la documentación de Fase 0 para reflejar el stack real.
