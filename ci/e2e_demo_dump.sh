#!/usr/bin/env bash
set -euo pipefail

DUMP_DIR="${EMX2_DUMP_DIR:-$HOME/emx2-demo-dump}"
DATABASE_DUMP="$DUMP_DIR/molgenis.pgc"
ROLES_DUMP="$DUMP_DIR/roles.sql.gz"
COMPANION_MARKER="$DUMP_DIR/cache-key.txt"
CACHE_KEY_FILE="${EMX2_DUMP_KEY_FILE:-emx2-demo-dump-key}"
JAR_GLOB="${EMX2_JAR_GLOB:-/workspace/build/libs/molgenis-emx2-*-all.jar}"
INITDB_SQL="${EMX2_INITDB_SQL:-.circleci/initdb.sql}"
CIRCLECI_CONFIG="${EMX2_CIRCLECI_CONFIG:-.circleci/config.yml}"
DATABASE="${EMX2_DATABASE:-molgenis}"
RESTORE_JOBS="${EMX2_RESTORE_JOBS:-4}"
KEY_MARKER_PREFIX="-- emx2-demo-dump-key: "
KEY_INPUTS_UNAVAILABLE="inputs-unavailable"
KEY_QUARANTINE_SUFFIX="dump-failed"

export PGHOST="${PGHOST:-127.0.0.1}"
export PGPORT="${PGPORT:-5432}"
export PGUSER="${PGUSER:-postgres}"
export PGPASSWORD="${PGPASSWORD:-postgres}"

if command -v sha256sum >/dev/null 2>&1; then
  SHA256=(sha256sum)
else
  SHA256=(shasum -a 256)
fi

announce() {
  echo "[demo-dump] $*" >&2
}

KEY_PATHSPEC=(backend data ':(exclude)data/_shacl' "$INITDB_SQL")

hashUntrackedInputs() {
  git ls-files -o --exclude-standard -z -- "${KEY_PATHSPEC[@]}" |
    while IFS= read -r -d '' untrackedPath; do
      "${SHA256[@]}" "$untrackedPath"
    done
}

postgresVersions() {
  echo "client $(pg_dump --version 2>/dev/null || echo unreadable)"
  echo "server $(psql -tAX -d postgres -c 'show server_version' 2>/dev/null || echo unreadable)"
}

demoIncludeFlags() {
  # The demo import is driven by whichever MOLGENIS_INCLUDE_* flags the CI
  # config exports before a fresh (non-restored) boot; read them from the
  # config itself so the key moves when a flag is added, removed or flipped.
  grep -oE 'MOLGENIS_INCLUDE_[A-Z_]+=[^[:space:]]*' "$CIRCLECI_CONFIG" 2>/dev/null |
    sort -u || echo "circleci-config-unreadable"
}

computeCacheKey() {
  local jarPath jarName committedInputs
  jarPath=$(ls -1 $JAR_GLOB 2>/dev/null | head -1 || true)
  if [ -z "$jarPath" ]; then
    announce "no jar matched $JAR_GLOB, so the EMX2 version cannot enter the key"
    echo "$KEY_INPUTS_UNAVAILABLE"
    return 0
  fi
  jarName=$(basename "$jarPath")
  committedInputs=$(git ls-files -s -- "${KEY_PATHSPEC[@]}" 2>/dev/null || true)
  if [ -z "$committedInputs" ]; then
    announce "git ls-files matched no file under backend/ or data/"
    echo "$KEY_INPUTS_UNAVAILABLE"
    return 0
  fi
  {
    echo "$jarName"
    postgresVersions
    demoIncludeFlags
    echo "$committedInputs"
    git diff HEAD -- "${KEY_PATHSPEC[@]}" 2>/dev/null || echo "uncommitted-changes-unreadable"
    hashUntrackedInputs || echo "untracked-files-unreadable"
  } | "${SHA256[@]}" | cut -d' ' -f1
}

readCacheKey() {
  head -1 "$CACHE_KEY_FILE"
}

readDumpMarker() {
  local firstLine
  firstLine=$(gzip -cd "$1" 2>/dev/null | head -1 || true)
  echo "${firstLine#"$KEY_MARKER_PREFIX"}"
}

resetDatabase() {
  announce "resetting $DATABASE so the backend can do a real demo import"
  psql -v ON_ERROR_STOP=1 -q -d postgres -c "DROP DATABASE IF EXISTS $DATABASE"
  psql -v ON_ERROR_STOP=1 -q -d postgres <<'SQL'
DO $$
DECLARE leftoverRole record;
BEGIN
  FOR leftoverRole IN SELECT rolname FROM pg_roles WHERE rolname LIKE 'MG\_%' OR rolname LIKE 'alias\_%' LOOP
    EXECUTE format('DROP ROLE %I', leftoverRole.rolname);
  END LOOP;
END
$$;
SQL
  psql -v ON_ERROR_STOP=1 -q -d postgres -c "DROP ROLE IF EXISTS $DATABASE"
  psql -v ON_ERROR_STOP=1 -q -d postgres -f "$INITDB_SQL"
}

rejectRestore() {
  announce "REJECTED: $1"
  announce "falling back to a full demo import, which is slow but correct"
}

restoreDump() {
  local cacheKey companionMarker rolesMarker
  cacheKey=$(computeCacheKey)
  if [ "$cacheKey" = "$KEY_INPUTS_UNAVAILABLE" ]; then
    rejectRestore "the cache key inputs are unavailable in this working directory"
    return 0
  fi
  if [ ! -f "$DATABASE_DUMP" ] || [ ! -f "$ROLES_DUMP" ] || [ ! -f "$COMPANION_MARKER" ]; then
    rejectRestore "no complete cached dump was restored into $DUMP_DIR"
    return 0
  fi
  companionMarker=$(head -1 "$COMPANION_MARKER")
  rolesMarker=$(readDumpMarker "$ROLES_DUMP")
  announce "cache key of this working tree:  $cacheKey"
  announce "cache key beside the dump:       $companionMarker"
  announce "cache key inside the roles dump: $rolesMarker"
  if [ "$companionMarker" != "$cacheKey" ] || [ "$rolesMarker" != "$cacheKey" ]; then
    rejectRestore "the dump was taken from a different backend/data tree"
    return 0
  fi
  if ! pg_restore --list "$DATABASE_DUMP" >/dev/null; then
    rejectRestore "this pg_restore cannot read the archive, so the database is left untouched"
    return 0
  fi
  if restoreDumpIntoPostgres; then
    announce "restored the cached dump, the backend will boot with no demo imports"
    if [ -n "${BASH_ENV:-}" ]; then
      echo "export MOLGENIS_DEMO_DUMP_RESTORED=true" >>"$BASH_ENV"
    else
      announce "BASH_ENV is unset so the restore cannot reach the boot step"
    fi
  else
    rejectRestore "loading the dump into postgres failed"
    resetDatabase
  fi
}

restoreDumpIntoPostgres() {
  psql -v ON_ERROR_STOP=1 -q -d postgres -c "DROP DATABASE IF EXISTS $DATABASE" &&
    gzip -cd "$ROLES_DUMP" | psql -v ON_ERROR_STOP=1 -q -d postgres &&
    pg_restore --create --clean --if-exists --exit-on-error --jobs "$RESTORE_JOBS" \
      -d postgres "$DATABASE_DUMP"
}

quarantineCacheKey() {
  announce "QUARANTINE: $1"
  rm -rf "$DUMP_DIR"
  mkdir -p "$DUMP_DIR"
  echo "$KEY_QUARANTINE_SUFFIX" >>"$CACHE_KEY_FILE"
}

saveDump() {
  local cacheKey
  if [ "${MOLGENIS_DEMO_DUMP_RESTORED:-false}" = "true" ]; then
    announce "this run restored the cache, so there is nothing new to dump"
    return 0
  fi
  cacheKey=$(readCacheKey)
  if [ "$cacheKey" = "$KEY_INPUTS_UNAVAILABLE" ]; then
    quarantineCacheKey "the cache key inputs were unavailable, refusing to publish a dump"
    return 0
  fi
  rm -rf "$DUMP_DIR"
  mkdir -p "$DUMP_DIR"
  if dumpPostgres "$cacheKey"; then
    announce "dumped $DATABASE under key $cacheKey"
    du -sh "$DUMP_DIR"
  else
    quarantineCacheKey "pg_dump failed, so no dump is published for the next run"
  fi
}

dropStatementsAboutRole() {
  awk -v role="$1" '
    BEGIN { quoted = "\"" role "\"" }
    $0 == "DROP ROLE IF EXISTS " role ";" { next }
    $0 == "DROP ROLE IF EXISTS " quoted ";" { next }
    $0 == "CREATE ROLE " role ";" { next }
    $0 == "CREATE ROLE " quoted ";" { next }
    index($0, "ALTER ROLE " role " WITH ") == 1 { next }
    index($0, "ALTER ROLE " quoted " WITH ") == 1 { next }
    { print }
  '
}

dumpPostgres() {
  {
    echo "$KEY_MARKER_PREFIX$1"
    pg_dumpall --roles-only --clean --if-exists | dropStatementsAboutRole "$PGUSER"
  } | gzip >"$ROLES_DUMP" &&
    pg_dump --format=custom --create --clean --if-exists -d "$DATABASE" -f "$DATABASE_DUMP" &&
    echo "$1" >"$COMPANION_MARKER"
}

case "${1:-}" in
key)
  computeCacheKey >"$CACHE_KEY_FILE"
  announce "postgres $(postgresVersions | tr '\n' ' ')"
  announce "cache key $(readCacheKey) written to $CACHE_KEY_FILE"
  ;;
restore)
  restoreDump
  ;;
save)
  saveDump
  ;;
*)
  echo "usage: $0 key|restore|save" >&2
  exit 2
  ;;
esac
