#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-http://host.docker.internal:8080}"
REPORTS="$ROOT/security/dast/reports"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for the ZAP baseline scan." >&2
  exit 1
fi

mkdir -p "$REPORTS"

echo "Running OWASP ZAP baseline against $TARGET"
set +e
docker run --rm -t \
  --add-host=host.docker.internal:host-gateway \
  -v "$ROOT/security/dast:/zap/wrk:rw" \
  ghcr.io/zaproxy/zap-stable:latest \
  zap-baseline.py -t "$TARGET" -c rules.tsv -r reports/zap-report.html -w reports/zap-report.md -J reports/zap-report.json
STATUS=$?
set -e

if [ "$STATUS" -ge 2 ]; then
  echo "ZAP reported failures. See $REPORTS/zap-report.html" >&2
  exit "$STATUS"
fi

echo "ZAP finished with exit code $STATUS. Report: $REPORTS/zap-report.html"
