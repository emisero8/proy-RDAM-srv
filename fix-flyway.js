const { Client } = require('pg');

const client = new Client({
    host: 'localhost',
    port: 5432,
    database: 'rdam',
    user: 'rdam_user',
    password: 'rdam_pass',
});

async function main() {
    await client.connect();

    // Verificar si existe la tabla flyway_schema_history
    const res = await client.query(`
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema='public' AND table_name='flyway_schema_history'
  `);

    if (res.rows.length > 0) {
        console.log('Tabla flyway_schema_history encontrada. Contenido:');
        const histRes = await client.query('SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank');
        histRes.rows.forEach(r => console.log(' -', JSON.stringify(r)));

        console.log('\nEliminando flyway_schema_history para que Flyway rehaga las migraciones...');
        await client.query('DROP TABLE flyway_schema_history');
        console.log('✅ Tabla eliminada. Ahora Flyway puede correr V1 desde cero.');
    } else {
        console.log('⚠️ No existe flyway_schema_history. Flyway creará las migraciones desde cero.');
    }

    await client.end();
}

main().catch(e => { console.error(e.message); client.end(); });
