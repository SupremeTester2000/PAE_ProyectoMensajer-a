#!/bin/bash
set -e

WORKSPACE_DIR="/home/alessandro/PAE_ProyectoMensajer-a"
PG_DATA="$WORKSPACE_DIR/postgres_db"
PG_BIN="/usr/lib/postgresql/16/bin"
PORT=5433

echo "Stopping any existing local postgres server..."
$PG_BIN/pg_ctl -D "$PG_DATA" stop -m immediate || true

echo "Cleaning old data directory..."
rm -rf "$PG_DATA"

echo "Initializing new PostgreSQL database cluster..."
$PG_BIN/initdb -D "$PG_DATA" -U alessandro --auth-local=trust --auth-host=trust

echo "Configuring PostgreSQL to use port $PORT..."
echo "port = $PORT" >> "$PG_DATA/postgresql.conf"
echo "unix_socket_directories = '$PG_DATA'" >> "$PG_DATA/postgresql.conf"

echo "Starting PostgreSQL server..."
$PG_BIN/pg_ctl -D "$PG_DATA" -l "$PG_DATA/logfile" start

echo "Waiting for PostgreSQL to start..."
sleep 3

echo "Creating database and user..."
psql -h localhost -p $PORT -d postgres -c "CREATE ROLE turkey WITH LOGIN PASSWORD 'kippycit0!';"
psql -h localhost -p $PORT -d postgres -c "ALTER ROLE turkey SUPERUSER;"
psql -h localhost -p $PORT -d postgres -c "CREATE DATABASE chatconnect OWNER turkey;"

echo "Applying schema..."
psql -h localhost -p $PORT -U turkey -d chatconnect -f "$WORKSPACE_DIR/schema.sql"

echo "Inserting test data..."
psql -h localhost -p $PORT -U turkey -d chatconnect <<EOF
-- Insert test users with password 'password123'
INSERT INTO users (name, email, password, status, connected) VALUES
  ('Juan Pérez', 'juan@test.com', '05HG/miF3ZiARo7xe5Q9XQ==:aYZbWR6LsmhTEJSj5yYPWH7oKBhByJWVsK7RdSE069U=', 'Disponible', false),
  ('María García', 'maria@test.com', 'KodoRQX3TxtyDA/3NqDlmw==:YWKM2r+NQ9c6Pz7SpstVB0HKUk/y+GxtygV9yG/rj5w=', 'Ocupado', false),
  ('Carlos López', 'carlos@test.com', 'FP2WB38oMceuQZgYnERQ0w==:lGhn9thM8YKYTv3TKKT0CqgoxgsreCp0t9W+3EmUfSI=', 'Inactivo', false)
ON CONFLICT DO NOTHING;

-- Insert conversations
INSERT INTO conversations (type, group_name, creation_date) VALUES
  ('PRIVATE', NULL, NOW()),
  ('PRIVATE', NULL, NOW()),
  ('GROUP', 'Equipo Proyecto 1', NOW())
ON CONFLICT DO NOTHING;

-- Insert participants
INSERT INTO participants_conversation (user_id, conversation_id, role) VALUES
  (1, 1, 'MEMBER'),  -- Juan en conversación 1
  (2, 1, 'MEMBER'),  -- María en conversación 1
  (1, 2, 'MEMBER'),  -- Juan en conversación 2
  (3, 2, 'MEMBER'),  -- Carlos en conversación 2
  (1, 3, 'MEMBER'),  -- Juan en grupo
  (2, 3, 'MEMBER')   -- María en grupo
ON CONFLICT DO NOTHING;

-- Insert messages
INSERT INTO messages (conversation_id, sender_id, content, status, timestamp) VALUES
  (1, 1, 'Hola María, ¿cómo estás?', 'SENT', NOW() - INTERVAL '5 minutes'),
  (1, 2, 'Hola Juan, muy bien', 'SENT', NOW() - INTERVAL '4 minutes'),
  (2, 1, 'Carlos, necesitamos hablar del proyecto', 'SENT', NOW() - INTERVAL '3 minutes'),
  (3, 1, 'Equipo, actualización del estado del proyecto', 'SENT', NOW() - INTERVAL '2 minutes'),
  (3, 2, 'Entendido, continuamos con la tarea', 'SENT', NOW() - INTERVAL '1 minute')
ON CONFLICT DO NOTHING;
EOF

echo "Verifying inserted data..."
psql -h localhost -p $PORT -U turkey -d chatconnect -c "SELECT COUNT(*) as usuarios FROM users;"
psql -h localhost -p $PORT -U turkey -d chatconnect -c "SELECT COUNT(*) as conversaciones FROM conversations;"
psql -h localhost -p $PORT -U turkey -d chatconnect -c "SELECT COUNT(*) as mensajes FROM messages;"

echo "PostgreSQL setup complete and running on port $PORT!"
