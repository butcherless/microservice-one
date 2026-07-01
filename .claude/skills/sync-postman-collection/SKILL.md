---
name: sync-postman-collection
description: Syncs docs/api/collection.json and docs/api/environment.json with the current springdoc-generated OpenAPI spec. Use this skill whenever the user asks to update, sync, or regenerate the Postman collection, or after adding, modifying, or removing any Spring MVC endpoint in CountryController. Also use it proactively after any change to src/main/kotlin/dev/cmartin/microserviceone/CountryController.kt. Do not skip it just because the user didn't say "Postman" — if endpoints changed, the collection should change too.
---

## What this skill does

Keeps `docs/api/collection.json` in sync with the live OpenAPI spec without destroying
hand-crafted E2E test scenarios. The pipeline:

1. Stop any running app instance
2. Build the executable JAR (always clean)
3. Generate the spec from a running instance of the JAR
4. Convert spec to a base collection
5. Sync the base collection into the existing `docs/api/collection.json` — also updates
   `docs/api/environment.json` and prints the change report in the same step:
   - **Resource folders** (grouped by OpenAPI tag — this project has a single tag,
     `country-controller`) — fully mirrored: new requests added, changed ones updated,
     removed ones deleted. Requests are matched by METHOD+path; if a folder has two
     requests on the same path (e.g. two GET examples with different query params),
     they're matched positionally in the order they appear. Event scripts (pre-request /
     test), on both individual requests and on the folder itself, are preserved on
     anything that still exists in the new spec.
   - **Folders no longer present in the new spec are never auto-deleted** — this
     covers both a renamed/removed OpenAPI tag and any folder a human added by hand
     (e.g. a "Health checks" folder unrelated to the generated spec). They're kept
     as-is and reported as "orphaned" so a human can decide whether to remove them.
   - **E2E folders** (name starts with `"E2E"`) — always preserved intact.
   - New path variables discovered in the spec are added to `docs/api/environment.json`;
     variables no longer referenced anywhere (including inside event scripts) are flagged
     as possibly unused, never removed automatically.

Newman is **not** run automatically. To smoke-test after syncing, run it manually scoped
to a folder, e.g.:

```bash
npx -y newman run docs/api/collection.json \
  -e docs/api/environment.json \
  --folder "country-controller"
```

---

## First-time setup (only if `docs/api/collection.json` doesn't exist yet)

Unlike a project with a hand-maintained collection already in place, this repo starts with
no Postman collection at all. Bootstrap empty stub files so Step 5 has something to sync
into — this makes the bootstrap run through the exact same merge code as every future sync,
instead of a separate one-off path that could drift from it:

```bash
mkdir -p docs/api

cat > docs/api/collection.json <<'EOF'
{
  "item": [],
  "event": [],
  "variable": [],
  "info": {
    "_postman_id": "<generate a fresh uuid4>",
    "name": "Microservice One API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  }
}
EOF

cat > docs/api/environment.json <<'EOF'
{
  "id": "<generate a fresh uuid4>",
  "name": "Microservice One — Local",
  "values": [
    { "key": "baseUrl", "value": "http://localhost:8081/ms-one", "type": "default", "enabled": true }
  ],
  "_postman_variable_scope": "environment"
}
EOF
```

Then run the normal pipeline (Steps 1-5 below). Every folder will be reported as "added"
on this first run — that's expected.

---

## Step 1 — Stop any running app instance

The app is always started with `java -jar target/microservice-one-<version>.jar`, so the
main class name never appears on the command line — match on the jar path instead:

```bash
pkill -f "target/microservice-one-.*\.jar" 2>/dev/null || true
```

---

## Step 2 — Build the executable JAR (clean first)

```bash
./mvnw clean package
```

Stop and report if the build fails — never proceed with a stale JAR. The JAR lands at
`target/microservice-one-<version>.jar` (exclude the `*.jar.original` file the Spring Boot
repackage plugin leaves behind).

---

## Step 3 — Generate the OpenAPI spec

This project's spec is served by springdoc-openapi from a **running instance** of the app,
not a build-time generator — start the server, wait for it to report healthy, fetch the
spec, then stop it again:

```bash
JAR=$(find target -maxdepth 1 -name "microservice-one-*.jar" ! -name "*.jar.original" | sort | tail -1)
java -jar "$JAR" > /tmp/sync-postman-app.log 2>&1 &
APP_PID=$!

for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/ms-one/actuator/health | grep -q 200 && break
  sleep 1
done

curl -s http://localhost:8081/ms-one/v3/api-docs.yaml -o /tmp/generated-openapi.yaml

kill "$APP_PID" 2>/dev/null || true
wait "$APP_PID" 2>/dev/null || true
```

Confirm `/tmp/generated-openapi.yaml` is non-empty before continuing. If the health check
never returns 200 within 30s, report that the app failed to start and show the tail of
`/tmp/sync-postman-app.log`.

`/v3/api-docs.yaml` depends on the `permitAll` rule in `SecurityConfiguration.kt` — if this
step gets a 403 body instead of YAML, check that rule first.

---

## Step 4 — Convert spec to a base collection

```bash
npx -y openapi-to-postmanv2 \
  -s /tmp/generated-openapi.yaml \
  -o /tmp/collection-base.json \
  -O folderStrategy=Tags,requestNameSource=summary,includeAuthInfoInExample=false
```

`folderStrategy=Tags` groups requests by OpenAPI tag, which is the resource name
(`country-controller` in this project — every operation in `CountryController.kt` shares
that tag).

---

## Step 5 — Sync into the existing collection

```bash
python3 .claude/skills/sync-postman-collection/scripts/sync.py \
  /tmp/collection-base.json \
  docs/api/collection.json \
  docs/api/environment.json
```

The script prints a change report listing every folder and request that was added,
updated, deleted, or orphaned, plus any environment variables that look unused. Read
the report and relay it to the user.

---

## After syncing

- Relay the change report to the user (folders added/orphaned, requests added/updated/deleted,
  possibly-unused environment variables).
- If any E2E scenarios reference endpoints that no longer exist, flag them by name.
- The sync only touches the working tree — `git diff docs/api/` before committing so the
  user can see exactly what changed, and `git checkout -- docs/api/` reverts cleanly if the
  sync produced something unexpected.
- Ask the user whether to commit the updated files before doing so.
