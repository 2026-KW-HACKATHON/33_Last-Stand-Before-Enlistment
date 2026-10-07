#!/usr/bin/env bash
# Linux CI only: synthetic data, no Supabase credentials or persistent volumes.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${DISCUSHION_TEST_DB_PASSWORD:?CI test password is required}"
container=discushion-ci-postgres
docker run --detach --network host --name "$container" \
  --env "POSTGRES_PASSWORD=$DISCUSHION_TEST_DB_PASSWORD" \
  --volume "$PWD:/workspace:ro" postgres:17.11 \
  -c listen_addresses=127.0.0.1 -c port=55432 >/dev/null

ready=false
for attempt in {1..60}; do
  if docker exec "$container" pg_isready -h 127.0.0.1 -p 55432 -U postgres >/dev/null; then
    ready=true
    break
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then
  docker logs "$container"
  exit 1
fi

psql_ci() {
  docker exec --env "PGPASSWORD=$DISCUSHION_TEST_DB_PASSWORD" "$container" \
    psql -X -h 127.0.0.1 -p 55432 -U postgres -v ON_ERROR_STOP=1 "$@"
}
psql_ci -d postgres -c 'CREATE DATABASE discushion_migration_test;'
psql_ci -d postgres -c 'CREATE DATABASE discushion_roles_test;'
for migration in supabase/migrations/*.sql; do
  psql_ci -d discushion_migration_test -f "/workspace/$migration"
done
psql_ci -d discushion_migration_test -f /workspace/supabase/tests/schema_integrity.sql
psql_ci -d discushion_roles_test -f /workspace/supabase/tests/api-role-isolation.sql
