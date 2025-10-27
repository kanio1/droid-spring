#!/usr/bin/env sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <url> <command...>" >&2
  exit 64
fi

TARGET="$1"
shift

MAX_ATTEMPTS="${WAIT_FOR_ATTEMPTS:-60}"
SLEEP_SECONDS="${WAIT_FOR_DELAY:-2}"

echo "[wait-for] Waiting for ${TARGET} (max attempts: ${MAX_ATTEMPTS}, delay: ${SLEEP_SECONDS}s)"

i=0
until [ "$i" -ge "$MAX_ATTEMPTS" ]; do
  if curl -sfL "$TARGET" >/dev/null 2>&1; then
    echo "[wait-for] ${TARGET} is available"
    exec "$@"
  fi
  i=$((i + 1))
  sleep "$SLEEP_SECONDS"
done

echo "[wait-for] Timeout waiting for ${TARGET}" >&2
exit 1
