# RDAM — Registro de Deudores Alimentarios Morosos
### i2T SA | Campus de Verano 2026
**Alumno:** Serovich Emilio — DNI 43.770.166 — 3º T.S "A"

---

## Descripción del Proyecto

**RDAM** es una plataforma web desarrollada para la gestión integral de solicitudes y certificados de libre deuda. Su objetivo es digitalizar completamente el trámite, eliminando la necesidad de asistencia presencial. El sistema permite a los ciudadanos solicitar y pagar sus certificados en línea, mientras que el personal interno revisa, dictamina y emite los documentos finales a través de un panel de gestión eficiente. Todo bajo un flujo lineal, transparente y seguro (Passwordless para los ciudadanos).

### Tecnologías Utilizadas
- **Frontend:** React / Vite (SPA) con Vanilla CSS.
- **Backend:** Java Spring Boot (API REST).
- **Base de Datos:** PostgreSQL (relacional, migración controlada con Flyway).
- **Infraestructura y Despliegue:** Docker y Docker Compose para levantar todo el entorno de manera aislada (Frontend, Backend, DB, Servidor de Correo y Pasarela de Pagos).
- **Integraciones:** Pasarela externa PlusPagos (Mock) y servidor SMTP (MailHog) para correos.

---

## Documentación Técnica

Todo el análisis funcional, técnico y guías operativas se encuentran adjuntos en este repositorio en formato PDF para su fácil lectura:

| Archivo | Descripción |
|---------|-------------|
| `ALCANCE-RDAM-Serovich_Emilio.pdf` | Documento de alcance general: Propósito, módulos funcionales, historias de usuario y flujos. |
| `DIAG_ARQUITECTURA-RDAM-Serovich_Emilio.pdf` | Diagrama visual (C4 Level 2) de la arquitectura del software y relaciones entre componentes. |
| `API_ENDPOINTS-RDAM-Serovich_Emilio.pdf` | Especificación de los endpoints del backend, rutas y métodos de comunicación. |
| `DB_RECOVERY-RDAM-Serovich_Emilio.pdf` | Procedimientos y estrategias para el respaldo (backup) y recuperación de la base de datos. |
| `Guia_Arranque-RDAM-Serovich_Emilio.pdf` | Guía detallada paso a paso para encender, detener y operar el sistema localmente. |
| `Certificado_Deudor_Alimenticio.pdf` | Archivo de muestra de un certificado final emitido por la plataforma. |

---

## Cómo Encender el Sistema (Resumen)

Para levantar toda la plataforma localmente, se recomienda seguir el archivo **`Guia_Arranque-RDAM-Serovich_Emilio.pdf`**. A continuación, se detalla un resumen de los comandos necesarios.

### Prerrequisitos
Tener instalado **Docker** y **Docker Compose**.

### Comandos de Ejecución

1. Abrí una terminal en la raíz de este proyecto.
2. Ejecutá el siguiente comando para construir e iniciar todos los servicios:
   ```bash
   docker-compose up -d --build
   ```
3. Esperá un par de minutos a que los contenedores inicien y la base de datos ejecute sus migraciones. Luego, podrás acceder a:
   - **Portal Ciudadano / Frontend:** [http://localhost:5173](http://localhost:5173)
   - **Backend API:** [http://localhost:8080](http://localhost:8080)
   - **Servidor de Correos (MailHog):** [http://localhost:8025](http://localhost:8025)
   - **Mock Pasarela PlusPagos:** [http://localhost:3000](http://localhost:3000)

4. Para detener y limpiar el sistema, ejecutá:
   ```bash
   docker-compose down
   ```

> **Nota:** El flujo comienza en el Portal Ciudadano creando una nueva solicitud. No requiere contraseñas, los códigos de acceso se enviarán al servidor de correos local (MailHog).
