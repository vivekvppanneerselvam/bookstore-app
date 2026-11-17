#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== Backend SAST: SpotBugs + FindSecBugs =="
(cd "$ROOT/backend" && mvn -B -Psecurity-scan verify -DskipTests)

echo
echo "== Frontend SCA: npm audit =="
(cd "$ROOT/frontend" && npm audit --audit-level=high)

echo
if command -v semgrep >/dev/null 2>&1; then
  echo "== Semgrep (optional, config auto) =="
  semgrep --config auto --error --quiet "$ROOT/backend/src" "$ROOT/frontend/src"
else
  echo "Semgrep is not installed; skipping. Install it with: brew install semgrep"
fi

echo
echo "Optional backend dependency scan (slow, downloads NVD data):"
echo "  cd backend && mvn org.owasp:dependency-check-maven:check"
