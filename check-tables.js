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
    const res = await client.query(`
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema='public' 
    ORDER BY table_name
  `);
    console.log('Tablas en BD rdam:');
    res.rows.forEach(row => console.log(' -', row.table_name));
    await client.end();
}

main().catch(e => { console.error(e.message); client.end(); });
