# GUÍA DE IMPLEMENTACIÓN — RDAM

## 1. Visión General

RDAM es un sistema web de gestión de solicitudes y certificados de libre deuda. Esta guía detalla cómo llevar el POC a producción, organizado en sprints incrementales.

**Stack recomendado:**
- **Frontend**: React/Next.js + Tailwind CSS
- **Backend**: Node.js (Express/Fastify) o .NET Core
- **Base de datos**: PostgreSQL
- **Storage**: S3/MinIO para PDFs
- **Email**: SendGrid o AWS SES
- **Pasarela**: PlusPagos API REST

## 2. Plan de Sprints

### Sprint 1 — Fundación (2 semanas)
| Tarea | HU | Estimación |
|-------|-----|------------|
| Configurar proyecto (monorepo, CI/CD) | — | 3 pts |
| Modelo de datos + migraciones | — | 5 pts |
| Autenticación y autorización (JWT + roles) | HU-09 | 8 pts |
| CRUD de usuarios internos | HU-09 | 5 pts |
| Login ciudadano (registro + login) | — | 5 pts |

### Sprint 2 — Solicitudes (2 semanas)
| Tarea | HU | Estimación |
|-------|-----|------------|
| Crear solicitud (formulario + API) | HU-01 | 8 pts |
| Listar solicitudes (ciudadano + interno) | HU-02, HU-05 | 8 pts |
| Detalle de solicitud con historial | HU-02 | 5 pts |
| Filtros y búsqueda | HU-05 | 3 pts |

### Sprint 3 — Flujo de Aprobación (2 semanas)
| Tarea | HU | Estimación |
|-------|-----|------------|
| Máquina de estados (transiciones) | — | 5 pts |
| Aprobar solicitud | HU-06 | 5 pts |
| Rechazar con observaciones | HU-07 | 5 pts |
| Notificaciones por email | — | 5 pts |
| Dashboard con métricas | — | 5 pts |

### Sprint 4 — Pagos y Emisión (2 semanas)
| Tarea | HU | Estimación |
|-------|-----|------------|
| Integración PlusPagos | HU-03 | 8 pts |
| Webhook de confirmación de pago | HU-03 | 5 pts |
| Generación de PDF (certificado) | HU-08 | 8 pts |
| Envío de certificado por email | HU-08 | 3 pts |
| Descarga de certificado | HU-04 | 3 pts |

### Sprint 5 — Estabilización (1 semana)
| Tarea | HU | Estimación |
|-------|-----|------------|
| Testing E2E | — | 8 pts |
| Auditoría y logging | — | 5 pts |
| Responsive y accesibilidad | — | 5 pts |
| Documentación de API | — | 3 pts |

## 3. Decisiones Técnicas

| Decisión | Opciones | Recomendación |
|----------|----------|---------------|
| Framework frontend | React, Vue, Angular | React/Next.js (SSR, ecosistema) |
| Framework backend | Express, Fastify, NestJS, .NET | NestJS o .NET Core según equipo |
| PDF Generation | Puppeteer, PDFKit, wkhtmltopdf | Puppeteer (fidelidad visual) |
| Autenticación | JWT, Session, OAuth | JWT + refresh tokens |
| Deploy | VPS, Docker, Kubernetes | Docker + CI/CD (GitHub Actions) |

## 4. Testing

| Tipo | Herramienta | Cobertura mínima |
|------|-------------|-----------------|
| Unit tests | Jest/xUnit | 80% servicios core |
| Integration tests | Supertest/E2E | Flujo completo de estados |
| E2E | Playwright | Happy path ciudadano + interno |
| Load testing | k6/Artillery | 500 req/s en endpoints principales |

### Tests críticos
1. Transición de estados: validar que no se salten pasos
2. Permisos: ciudadano no puede aprobar, interno no puede pagar
3. Pago: webhook idempotente, manejo de timeout
4. PDF: generación sin errores con datos edge case

## 5. Deployment

### Ambientes
| Ambiente | Propósito | URL |
|----------|-----------|-----|
| DEV | Desarrollo continuo | dev.rdam.gob.ar |
| STG | QA y UAT | stg.rdam.gob.ar |
| PROD | Producción | rdam.gob.ar |

### Variables de entorno principales
```env
DATABASE_URL=postgresql://user:pass@host:5432/rdam
JWT_SECRET=<secret>
PLUSPAGOS_API_KEY=<key>
PLUSPAGOS_SECRET=<secret>
PLUSPAGOS_WEBHOOK_SECRET=<webhook_secret>
SMTP_HOST=<host>
SMTP_USER=<user>
SMTP_PASS=<pass>
S3_BUCKET=rdam-certificados
S3_REGION=us-east-1
```

## 6. Checklist de Entrega

### Pre-QA
- [ ] Todos los tests pasan
- [ ] Lint sin warnings
- [ ] API documentada (Swagger/OpenAPI)
- [ ] Variables de entorno configuradas en STG
- [ ] Datos seed cargados

### Pre-PROD
- [ ] UAT aprobado por product owner
- [ ] Tests de carga superados
- [ ] Backup de DB configurado
- [ ] Monitoreo y alertas configurados
- [ ] Certificados SSL instalados
- [ ] DNS configurado
- [ ] Runbook de operaciones documentado
