---
name: validate-openapi
description: >
  Validates the OpenAPI YAML spec that springdoc-openapi generates at runtime from the
  Spring MVC controller definitions in this Kotlin/Spring Boot project. Use this skill
  whenever the user asks to validate, check, verify, or lint the OpenAPI spec, API
  contract, or springdoc/swagger output — even if they say "run the openapi check", "does
  our spec pass?", "check the api doc", or "verify the generated yaml". Also trigger when
  the user adds or changes a controller endpoint and wants to confirm the spec is still
  valid.
---

# validate-openapi

Validates the OpenAPI 3.1 spec that springdoc-openapi generates at runtime from the Kotlin
Spring MVC controller definitions. The pipeline has six steps, plus a final report:
**stop → build → generate → redocly → schema-check → spectral → report**.

## Primary execution — run the script

Always try the bundled script first. It runs all six steps in one shot and is faster than
issuing individual tool calls:

```bash
bash .claude/skills/validate-openapi/scripts/run.sh
```

Read the script output and present the report to the user exactly as printed.

If the script exits with a non-zero code due to a **script bug** (not a validation failure —
check whether the error is in the bash logic itself, e.g. a missing tool, a bad path, or a
jq syntax error), fix the script, then re-run it. Only fall back to executing the steps
manually (as described below) if the script cannot be fixed quickly.

## Step 1 — Stop any running application instance

Find and kill any running instance of the packaged jar. The app is always started with
`java -jar target/microservice-one-<version>.jar`, so the main class name never appears on
the command line — match on the jar path instead:

```bash
pkill -f "target/microservice-one-.*\.jar" 2>/dev/null || true
```

To inspect before killing: `pgrep -fl "target/microservice-one-.*\.jar"`

Safe to run when nothing is running; the `|| true` suppresses the non-zero exit.

## Step 2 — Build the executable JAR (clean first)

```bash
./mvnw clean package
```

`clean` wipes `target/` before rebuilding, so the JAR is built entirely from current source
with no stale artifacts from earlier compilations. This also runs the unit test suite
(Surefire); Failsafe integration tests and the JaCoCo report are skipped here since they're
unrelated to spec validation. The executable JAR lands at
`target/microservice-one-<version>.jar` — the Spring Boot repackage plugin renames the plain
artifact to `*.jar.original`, so exclude that suffix when searching.

If the build fails, stop and report the compile/test errors — there is no point running
validation against a stale or missing spec.

## Step 3 — Generate the spec

Unlike a build-time generator, this project's spec is served by springdoc-openapi from a
**running instance** of the app. Generating it means starting the server, waiting for it to
report healthy, fetching the spec over HTTP, then stopping the server again:

```bash
JAR=$(find target -maxdepth 1 -name "microservice-one-*.jar" ! -name "*.jar.original" | sort | tail -1)
java -jar "$JAR" > /tmp/validate-openapi-app.log 2>&1 &
APP_PID=$!

for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/ms-one/actuator/health | grep -q 200 && break
  sleep 1
done

curl -s http://localhost:8081/ms-one/v3/api-docs.yaml -o /tmp/generated-openapi.yaml

kill "$APP_PID" 2>/dev/null || true
wait "$APP_PID" 2>/dev/null || true
```

If the health check never returns 200 within 30s, report that the app failed to start and
show the tail of `/tmp/validate-openapi-app.log`. If `/tmp/generated-openapi.yaml` is empty
or missing after the curl, report that the endpoint produced no output.

`/v3/api-docs.yaml` must be permitted in `SecurityConfiguration.kt` alongside
`/v3/api-docs/**` (the two are separate Ant patterns — one does not imply the other). If this
step starts returning a 403 body instead of a spec, check that rule first; every downstream
step will fail confusingly otherwise.

## Step 4 — Validate with Redocly (OAS 3.1 schema conformance)

```bash
npx -y @redocly/cli@latest lint /tmp/generated-openapi.yaml \
  --config redocly.yaml \
  --format json \
  2>/tmp/redocly.stderr > /tmp/redocly-report.json || true
```

Redocly's CLI exits with status **1 whenever it finds any lint error** — that's the normal
case this whole skill exists to catch, not a tool failure. The `|| true` is required so the
shell doesn't abort here; do not drop it. Confirm the report is actually valid JSON before
parsing it (`jq empty /tmp/redocly-report.json`) — if it isn't, the tool itself failed and
`/tmp/redocly.stderr` has the reason.

`redocly.yaml` enforces `struct: error` — strict schema conformance only. Parse the JSON:
- `totals.errors` — must be 0 to pass
- `problems[]` with `severity == "error"` — each one needs fixing

## Step 5 — Schema completeness check

Verify that every request body and response that carries a JSON object uses a `$ref` to
`components/schemas` rather than an inline object definition. Simple scalars (`type: string`,
`type: integer`, etc.) and arrays whose `items` already point to a `$ref` are allowed inline.

Convert the YAML to JSON first (requires no installed packages — uses npx cache):

```bash
npx -y js-yaml /tmp/generated-openapi.yaml > /tmp/generated-openapi.json
```

Then check for inline object schemas using the same jq filter `run.sh` uses (kept in one
place at `.claude/skills/validate-openapi/scripts/inline-schema-check.jq` so the manual
path and the scripted path can never drift apart):

```bash
INLINE_ISSUES=$(jq -r -f .claude/skills/validate-openapi/scripts/inline-schema-check.jq \
  /tmp/generated-openapi.json)
```

If `$INLINE_ISSUES` is non-empty, each line identifies an endpoint and media type where an
object schema is defined inline. For each, find the matching handler in
`src/main/kotlin/dev/cmartin/microserviceone/CountryController.kt`, extract the schema into a
dedicated data class in `Model.kt`, and let Jackson/springdoc derive `components/schemas` from
it via `$ref`.

## Step 6 — Lint with Spectral (best-practice rules)

```bash
npx -y @stoplight/spectral-cli lint /tmp/generated-openapi.yaml \
  --ruleset .spectral.yaml \
  --format json \
  --output /tmp/spectral-report.json
```

`.spectral.yaml` extends `spectral:oas`. In the output array:
- `severity == 0` → error (blocks the build)
- `severity == 1` → warning (informational)

## Reporting results

Before summarizing, extract the OpenAPI version declared in the generated spec and fetch
the latest stable version from the OAI repository to confirm they match:

```bash
# Version declared in the spec
APP_VERSION=$(grep -m1 "^openapi:" /tmp/generated-openapi.yaml | awk '{print $2}')

# Latest stable version from the official OAI GitHub release (falls back gracefully
# if gh isn't authenticated or the network is unavailable — this is a nice-to-have,
# not a validation failure)
LATEST_VERSION=$(gh api repos/OAI/OpenAPI-Specification/releases/latest --jq '.tag_name' 2>/dev/null || echo "unknown")
```

Include all results in the report header:

```
OpenAPI spec version : X.Y.Z  (latest stable: A.B.C)
Redocly              : X errors, Y warnings
Schema check         : X inline object schemas found
Spectral             : X errors, Y warnings
```

If the spec version is behind the latest, note it — but do not treat it as a failure.
If `Schema check` is non-zero, list each violation and treat it as an error.

Then print the endpoint inventory as a markdown table, grouped by resource (OpenAPI tag —
this project has a single controller, so every operation is tagged `country-controller`) and
sorted by path, using the same jq filter `run.sh` uses
(`.claude/skills/validate-openapi/scripts/endpoint-table.jq`):

```bash
jq -r -f .claude/skills/validate-openapi/scripts/endpoint-table.jq /tmp/generated-openapi.json \
  | sort -t$'\t' -k1,1 -k3,3
```

Format the output as:

```
| #  | Resource          | Method | Path              |
|----|--------------------|--------|--------------------|
|  1 | country-controller | GET    | /countries         |
|  2 | country-controller | GET    | /countries/{code}  |
| …  | …                  | …      | …                  |
```

**If errors exist**, for each one show:
- The rule / code (e.g., `operation-operationId`, `no-path-trailing-slash`)
- The JSON pointer where it failed (e.g., `/paths/~1countries~1{code}/get`)
- The human-readable message

Then trace the pointer back to the relevant handler. All endpoints live in one file:
`src/main/kotlin/dev/cmartin/microserviceone/CountryController.kt`. Map the path and HTTP
method from the pointer to the matching `@GetMapping` function and explain what to change.

**If no errors**, confirm with a one-line summary:
```
PASSED — Redocly 0 errors, Spectral 0 errors (N warnings)
```

## Notes

- `npx -y` auto-installs tools on first run; subsequent runs use the npm cache.
- Both `jq` and Node/npm are assumed available in this dev environment.
- The generated YAML and reports are written to `/tmp/` and never committed.
- The app instance started in Step 3 is stopped before the script exits (success or
  failure). If the script is interrupted mid-run (e.g. Ctrl-C during Step 3), a stray
  `java -jar target/microservice-one-*.jar` process may be left running — Step 1 of the
  next run will clean it up.
