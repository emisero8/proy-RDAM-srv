# 🚀 RDAM — Guía de Arranque del Sistema

**Registro de Deudores Alimentarios Morosos · Campus de Verano 2026**

Esta guía explica cómo levantar el sistema completo desde cero mediante consola en Windows (PowerShell) o Linux/macOS (bash). Los comandos son los mismos salvo que se indique lo contrario.

---

## 1. Prerrequisitos

| Herramienta     | Versión mínima | Verificar con           |
|-----------------|---------------|--------------------------|
| Java (JDK)      | 21            | `java -version`          |
| Maven           | 3.9+          | `mvn -version`           |
| Node.js         | 20 LTS        | `node -v`                |
| npm             | 10+           | `npm -v`                 |
| PostgreSQL      | 15+           | `psql --version`         |

> Si Maven no está disponible como comando global, podés usar el wrapper incluido: `./mvnw` (Linux/macOS) o `mvnw.cmd` (Windows).

---

## 2. Configuración inicial de PostgreSQL

Estos pasos se realizan **una sola vez** al instalar el sistema.

### 2.1 Crear usuario y base de datos

Abrí `psql` como superusuario (ej. `postgres`) y ejecutá:

```sql
-- Crear usuario de la aplicación
CREATE USER rdam_user WITH PASSWORD 'rdam_pass';

-- Crear base de datos
CREATE DATABASE rdam OWNER rdam_user;

-- Otorgar permisos
GRANT ALL PRIVILEGES ON DATABASE rdam TO rdam_user;
```

Para conectarte a psql como superusuario:
```powershell
# Windows (ajustar path si es necesario)
psql -U postgres

# Linux/macOS
sudo -u postgres psql
```

### 2.2 Aplicar el esquema y datos semilla

La aplicación aplica el esquema automáticamente al arrancar mediante **Flyway**.
Si querés aplicarlo manualmente o reiniciar la base, ejecutá el DDL completo:

```powershell
# Desde la raíz del proyecto
psql -U rdam_user -d rdam -f DDL.sql
```

> ⚠️ `DDL.sql` es idempotente (limpia y recrea todo). Si ya hay datos, se pierden.

---

## 3. Variables de entorno (opcional)

El sistema funciona con valores por defecto en desarrollo. Para producción o para sobrescribir los defaults, podés definir estas variables antes de arrancar el backend:

| Variable                  | Default                  | Descripción                    |
|---------------------------|--------------------------|--------------------------------|
| `DB_USERNAME`             | `rdam_user`             | Usuario PostgreSQL              |
| `DB_PASSWORD`             | `rdam_pass`             | Contraseña PostgreSQL           |
| `JWT_SECRET`              | *(valor dev)*            | Clave JWT (mín. 32 chars)      |
| `MAIL_USERNAME`           | `noreply@rdam.gob.ar`   | Cuenta SMTP para notificaciones |
| `MAIL_PASSWORD`           | *(vacío)*                | Contraseña SMTP                 |
| `PLUSPAGOS_URL`           | `http://localhost:3000` | URL de la pasarela de pagos     |
| `CERT_STORAGE_PATH`       | `./certs`               | Directorio de certificados PDF  |

En PowerShell (sesión actual):
```powershell
$env:DB_PASSWORD = "mi_password_seguro"
```

---

## 4. Levantar la Pasarela de Pagos (PlusPagos Mock)

```powershell
cd pasarela-campus-2026\pluspagos-mock-simple
npm start
```

✅ Escucha en: **http://localhost:3000**

> Mantener esta terminal abierta. La pasarela simula PlusPagos para desarrollo.

---

## 5. Levantar el Backend (Spring Boot)

```powershell
cd backend
```

### Perfil de desarrollo (con datos de prueba, logs verbose):
```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Perfil de producción (requiere PostgreSQL y variables de entorno configuradas):
```powershell
mvn spring-boot:run
```

✅ El backend estará listo cuando veas en los logs:
```
Started RdamApplication in X.XXX seconds
```

✅ API disponible en: **http://localhost:8080/v1**

✅ Health check: **http://localhost:8080/v1/actuator/health**

> Flyway aplica las migraciones automáticamente al arrancar.
> Si es la primera vez, creará todas las tablas en la BD `rdam`.

---

## 6. Levantar el Frontend (React + Vite)

En una nueva terminal:

```powershell
cd frontend

# Primera vez: instalar dependencias
npm install

# Arrancar servidor de desarrollo
npm run dev
```

✅ Frontend disponible en: **http://localhost:5173**

---

## 7. Orden de arranque recomendado

```
1. PostgreSQL (debe estar corriendo como servicio)
2. Pasarela PlusPagos Mock     → cd pasarela-campus-2026\pluspagos-mock-simple && npm start
3. Backend Spring Boot         → cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
4. Frontend React              → cd frontend && npm run dev
```

---

## 8. Usuarios de prueba

> Estos usuarios se crean automáticamente al arrancar con perfil `dev` (si la BD está vacía).
> Para producción, usar el DDL.sql que incluye datos semilla con bcrypt.

| Rol        | Email                     | Contraseña      | Portal de login         |
|------------|---------------------------|-----------------|-------------------------|
| Admin      | admin@rdam.gob.ar         | `Password1!`   | http://localhost:5173/admin/login |
| Gestor     | lmartinez@rdam.gob.ar     | `Password1!`   | http://localhost:5173/admin/login |
| Ciudadano  | mgarcia@email.com         | *(sin contraseña — login por email)* | http://localhost:5173/login |

---

## 9. URLs del sistema

| Componente       | URL                                    |
|------------------|----------------------------------------|
| Frontend         | http://localhost:5173                  |
| API Backend      | http://localhost:8080/v1               |
| Health Check     | http://localhost:8080/v1/actuator/health |
| Pasarela Mock    | http://localhost:3000                  |

---

## 10. Solución de problemas frecuentes

### Error: `Connection refused` al arrancar el backend
- Verificar que PostgreSQL esté corriendo: `pg_ctl status` o revisar Servicios de Windows.
- Verificar que el usuario y la BD existen: `psql -U rdam_user -d rdam -c "\l"`

### Error: `Flyway migration failed`
- Si la BD tiene tablas de una versión anterior incompatible, limpiar y reaplicar:
  ```powershell
  psql -U rdam_user -d rdam -f DDL.sql
  ```

### Puerto ya en uso
- Backend en 8080: `netstat -ano | findstr :8080` (Windows) o `lsof -i :8080` (Linux/macOS)
- Frontend en 5173: Vite elegirá automáticamente el siguiente puerto disponible.

### `npm install` falla por permisos (Windows)
```powershell
npm install --legacy-peer-deps
```

---

## 11. Arranque con Docker (Entrega Final)

Para facilitar la evaluación, todo el proyecto (Base de Datos, Pasarela de Pagos, Backend y Frontend) está contenedorizado.

Solo necesitás tener **Docker Desktop** instalado.

1. Abrí una terminal en la raíz del proyecto (`proy-RDAM-srvff`).
2. Ejecutá:
   ```powershell
   docker compose up --build
   ```
3. Esperá a que todos los contenedores inicien y muestren `healthy` (puede tardar un par de minutos la primera vez mientras compila el backend y frontend).
4. El sistema estará disponible en:
   - **Frontend**: http://localhost:5173
   - **Backend API**: http://localhost:8080/v1
   - **Pasarela Mock**: http://localhost:3000

> 💡 **Nota**: Docker inicializará automáticamente la base de datos PostgreSQL con el DDL y los usuarios semilla de prueba (`admin@rdam.gob.ar`, `lmartinez@rdam.gob.ar`, `mgarcia@email.com` - contraseña: `Password1!`).

---

*Última actualización: Marzo 2026 — Campus de Verano 2026 · i2T Software Factory*
