// Limpia la BD y aplica el schema correcto (V1__init_schema.sql de Flyway)
const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  host: 'localhost',
  port: 5432,
  database: 'rdam',
  user: 'rdam_user',
  password: 'rdam_pass',
});

async function main() {
  await client.connect();
  console.log('✅ Conectado a PostgreSQL.');

  // 1. Limpiar schema existente
  console.log('🧹 Limpiando schema existente...');
  await client.query(`
    DROP TABLE IF EXISTS refresh_tokens CASCADE;
    DROP TABLE IF EXISTS certificados CASCADE;
    DROP TABLE IF EXISTS pagos CASCADE;
    DROP TABLE IF EXISTS historial_estados CASCADE;
    DROP TABLE IF EXISTS solicitudes CASCADE;
    DROP TABLE IF EXISTS usuarios CASCADE;
    DROP VIEW IF EXISTS v_bandeja_interna;
    DROP TABLE IF EXISTS flyway_schema_history CASCADE;
  `);
  console.log('✅ Schema limpiado.');

  // 2. Aplicar el schema correcto de Flyway (V1__init_schema.sql)
  const schemaPath = path.join(__dirname, 'backend', 'src', 'main', 'resources', 'db', 'migration', 'V1__init_schema.sql');
  const sql = fs.readFileSync(schemaPath, 'utf-8');
  console.log('📋 Aplicando V1__init_schema.sql...');
  await client.query(sql);
  console.log('✅ Schema aplicado correctamente.');

  // 3. Cargar datos semilla de desarrollo (usuarios de prueba)
  console.log('🌱 Insertando datos semilla...');
  const bcryptHash = '$2b$10$/qfmOBdlZw7iryK73cWS2.Cd3mL48vFXXhbDmW6mRGSiUJCaliKda'; // "Password1!"

  await client.query(`
    INSERT INTO usuarios (nombre, apellido, email, password, dni_cuil, tipo, rol)
    VALUES 
      ('Admin', 'Sistema', 'admin@rdam.gob.ar', '${bcryptHash}', '20-00000001-0', 'INTERNO', 'ADMIN'),
      ('Laura', 'Martinez', 'lmartinez@rdam.gob.ar', '${bcryptHash}', '27-12345678-9', 'INTERNO', 'GESTOR'),
      ('Maria', 'Garcia', 'mgarcia@email.com', '${bcryptHash}', '27-34567890-1', 'CIUDADANO', 'CIUDADANO')
    ON CONFLICT (email) DO NOTHING;
  `);
  console.log('✅ Datos semilla insertados. Usuarios: admin@rdam.gob.ar, lmartinez@rdam.gob.ar, mgarcia@email.com');
  console.log('🔑 Contraseña para todos: Password1!');

  await client.end();
}

main().catch(async e => {
  console.error('❌ Error:', e.message);
  await client.end();
  process.exit(1);
});
