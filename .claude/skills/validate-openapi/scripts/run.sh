#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
cd "$PROJECT_ROOT"

SPEC=/tmp/generated-openapi.yaml
SPEC_JSON=/tmp/generated-openapi.json
REDOCLY_REPORT=/tmp/redocly-report.json
SPECTRAL_REPORT=/tmp/spectral-report.json
BUILD_LOG=/tmp/validate-openapi-build.log
APP_LOG=/tmp/validate-openapi-app.log
REDOCLY_STDERR=/tmp/validate-openapi-redocly.stderr

BASE_URL="http://localhost:8081/ms-one"
APP_PID=""

# Always stop whatever instance this script started, even on failure/interrupt.
cleanup() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ── 1. Stop ───────────────────────────────────────────────────────────────────
echo "==> [1/6] Stopping any running instance..."
pkill -f "target/microservice-one-.*\.jar" 2>/dev/null || true
sleep 1

# ── 2. Build ──────────────────────────────────────────────────────────────────
echo "==> [2/6] Building executable JAR (clean first)..."
set +e
./mvnw clean package > "$BUILD_LOG" 2>&1
BUILD_EXIT=$?
set -e
if [ "$BUILD_EXIT" -ne 0 ]; then
  echo "ERROR: build failed (exit $BUILD_EXIT) — full log:" >&2
  cat "$BUILD_LOG" >&2
  exit 1
fi
tail -3 "$BUILD_LOG"

JAR=$(find target -maxdepth 1 -name "microservice-one-*.jar" ! -name "*.jar.original" | sort | tail -1)
if [ -z "$JAR" ]; then
  echo "ERROR: executable JAR not found" >&2; exit 1
fi

# ── 3. Generate ───────────────────────────────────────────────────────────────
echo "==> [3/6] Starting app and fetching generated OpenAPI spec..."
java -jar "$JAR" > "$APP_LOG" 2>&1 &
APP_PID=$!

UP=0
for _ in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null | grep -q 200; then
    UP=1
    break
  fi
  sleep 1
done

if [ "$UP" -ne 1 ]; then
  echo "ERROR: app did not become healthy within 30s — log tail:" >&2
  tail -30 "$APP_LOG" >&2
  exit 1
fi

curl -s "$BASE_URL/v3/api-docs.yaml" -o "$SPEC"

kill "$APP_PID" 2>/dev/null || true
wait "$APP_PID" 2>/dev/null || true
APP_PID=""

if [ ! -s "$SPEC" ]; then
  echo "ERROR: spec generation produced no output — app log tail:" >&2
  tail -30 "$APP_LOG" >&2
  exit 1
fi

# ── 4. Redocly ────────────────────────────────────────────────────────────────
echo "==> [4/6] Validating with Redocly..."
# redocly exits 1 whenever it finds lint errors, not just on tool failure — that's
# the normal case this script exists to report, so don't let `set -e` kill us here.
npx -y @redocly/cli@latest lint "$SPEC" \
  --config redocly.yaml --format json \
  2>"$REDOCLY_STDERR" > "$REDOCLY_REPORT" || true

if ! jq empty "$REDOCLY_REPORT" >/dev/null 2>&1; then
  echo "ERROR: Redocly did not produce valid JSON output — stderr:" >&2
  cat "$REDOCLY_STDERR" >&2
  exit 1
fi

REDOCLY_ERRORS=$(jq '.totals.errors' "$REDOCLY_REPORT")
REDOCLY_WARNINGS=$(jq '.totals.warnings' "$REDOCLY_REPORT")

# ── 5. Schema check ───────────────────────────────────────────────────────────
echo "==> [5/6] Checking schema completeness..."
npx -y js-yaml "$SPEC" > "$SPEC_JSON" 2>/dev/null

INLINE_ISSUES=$(jq -r -f "$SCRIPT_DIR/inline-schema-check.jq" "$SPEC_JSON" 2>/dev/null || true)
INLINE_COUNT=$([ -n "$INLINE_ISSUES" ] && echo "$INLINE_ISSUES" | wc -l | tr -d ' ' || echo 0)

# ── 6. Spectral ───────────────────────────────────────────────────────────────
echo "==> [6/6] Linting with Spectral..."
npx -y @stoplight/spectral-cli lint "$SPEC" \
  --ruleset .spectral.yaml --format json \
  --output "$SPECTRAL_REPORT" 2>/dev/null || true

SPECTRAL_ERRORS=$(jq '[.[] | select(.severity == 0)] | length' "$SPECTRAL_REPORT")
SPECTRAL_WARNINGS=$(jq '[.[] | select(.severity == 1)] | length' "$SPECTRAL_REPORT")

# ── Report ────────────────────────────────────────────────────────────────────
APP_VERSION=$(grep -m1 "^openapi:" "$SPEC" | awk '{print $2}')
LATEST_VERSION=$(gh api repos/OAI/OpenAPI-Specification/releases/latest --jq '.tag_name' 2>/dev/null || echo "unknown")

echo ""
echo "================================================================"
echo " OpenAPI spec version : $APP_VERSION  (latest stable: $LATEST_VERSION)"
echo " Redocly              : $REDOCLY_ERRORS errors, $REDOCLY_WARNINGS warnings"
echo " Schema check         : $INLINE_COUNT inline object schemas found"
echo " Spectral             : $SPECTRAL_ERRORS errors, $SPECTRAL_WARNINGS warnings"
echo "================================================================"

if [ "$REDOCLY_ERRORS" -gt 0 ]; then
  echo ""
  echo "Redocly errors:"
  jq -r '.problems[] | select(.severity == "error") | "  [\(.ruleId)] \(.message) @ \(.location[0].pointer // "/")"' "$REDOCLY_REPORT"
fi

if [ -n "$INLINE_ISSUES" ]; then
  echo ""
  echo "Inline object schema violations:"
  echo "$INLINE_ISSUES"
fi

if [ "$SPECTRAL_ERRORS" -gt 0 ]; then
  echo ""
  echo "Spectral errors:"
  jq -r '.[] | select(.severity == 0) | "  [\(.code)] \(.message) @ /\(.path | join("/"))"' "$SPECTRAL_REPORT"
fi

TOTAL_ERRORS=$((REDOCLY_ERRORS + INLINE_COUNT + SPECTRAL_ERRORS))

echo ""
if [ "$TOTAL_ERRORS" -eq 0 ]; then
  echo "PASSED — no errors found"
  if [ "$APP_VERSION" != "$LATEST_VERSION" ]; then
    echo "NOTE  — spec $APP_VERSION is behind latest $LATEST_VERSION"
  fi
else
  echo "FAILED — $TOTAL_ERRORS error(s) must be fixed"
fi

# ── Endpoint table ─────────────────────────────────────────────────────────────
echo ""
echo "Endpoints:"
echo ""
printf "| %-3s | %-18s | %-6s | %s\n" "#" "Resource" "Method" "Path"
printf "|-----|--------------------|--------|%s\n" "-----------------------------------"

i=1
while IFS=$'\t' read -r resource method path; do
  printf "| %-3s | %-18s | %-6s | %s\n" "$i" "$resource" "$method" "$path"
  i=$((i+1))
done < <(jq -r -f "$SCRIPT_DIR/endpoint-table.jq" "$SPEC_JSON" | sort -t$'\t' -k1,1 -k3,3)

exit $( [ "$TOTAL_ERRORS" -eq 0 ] && echo 0 || echo 1 )
