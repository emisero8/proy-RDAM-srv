// Script temporal para aplicar el DDL.sql a la BD PostgreSQL
// Uso: node apply-ddl.js
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
  try {
    console.log('Conectando a PostgreSQL...');
    await client.connect();
    console.log('✅ Conectado.');

    const ddlPath = path.join(__dirname, 'DDL.sql');
    const sql = fs.readFileSync(ddlPath, 'utf-8');

    console.log('Aplicando DDL.sql...');
    await client.query(sql);
    console.log('✅ DDL aplicado correctamente. Todas las tablas y datos semilla fueron creados.');
  } catch (err) {
    console.error('❌ Error al aplicar el DDL:', err.message);
    process.exit(1);
  } finally {
    await client.end();
  }
}

main();
