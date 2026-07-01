#!/usr/bin/env python3
"""
Syncs a Postman Collection v2.1 with a freshly generated base collection.

Rules:
- Resource folders (all folders whose name does NOT start with "E2E"): fully synced.
  Requests matched by METHOD+path are updated; unmatched old ones are deleted; new ones added.
  Event scripts (pre-request / test) on matched requests are preserved from the old collection.
  A folder-level pre-request/test script (set on the folder itself) is also preserved.
- Folders present in the old collection but absent from the new spec are NEVER deleted
  automatically (this includes both renamed/removed Tapir tags and any folder a human added
  by hand, e.g. a "Health checks" folder). They are kept as-is and flagged as "orphaned" in
  the report so a human can decide whether to remove them.
- E2E folders (name starts with "E2E"): always preserved unchanged.
- Environment file: new path variables discovered in the synced collection are added (never
  removed automatically); variables no longer referenced by any request are flagged in the
  report as possibly unused, but left in place.
"""

import json
import sys
from pathlib import Path
from typing import Optional

E2E_PREFIX = "E2E"


def _path_from_raw(raw: str) -> str:
    """Best-effort path extraction from a raw URL string (used only when structured
    path segments are unavailable). Strips query string and a leading {{baseUrl}}
    variable so the result is shaped like the segment-joined path used elsewhere."""
    without_query = raw.split("?", 1)[0]
    if "{{baseUrl}}" in without_query:
        without_query = without_query.split("{{baseUrl}}", 1)[1]
    return without_query


def request_key(item: dict) -> str:
    """Stable identity for a request: 'METHOD /path/segments'."""
    req = item.get("request", {})
    method = req.get("method", "").upper()
    url = req.get("url", {})
    if isinstance(url, str):
        path = _path_from_raw(url)
    else:
        segments = url.get("path", [])
        if segments:
            path = "/" + "/".join(str(s) for s in segments)
        else:
            path = _path_from_raw(url.get("raw", ""))
    return f"{method} {path}"


def sync_folder(new_folder: dict, old_folder: Optional[dict], changes: dict) -> dict:
    """Merge new_folder items with preserved scripts from old_folder."""
    folder_name = new_folder["name"]
    old_folder = old_folder or {}

    # Duplicate METHOD+path keys are matched positionally (FIFO) rather than by
    # dict overwrite, so two old requests sharing a key (e.g. two GET examples on
    # the same path with different query params) don't silently lose one script.
    queues: dict[str, list[dict]] = {}
    for old_item in old_folder.get("item", []):
        queues.setdefault(request_key(old_item), []).append(old_item)

    synced_items = []
    for new_item in new_folder.get("item", []):
        key = request_key(new_item)
        queue = queues.get(key)
        if queue:
            old_item = queue.pop(0)
            merged = dict(new_item)
            old_events = old_item.get("event")
            if old_events:
                merged["event"] = old_events
            # openapi-to-postmanv2 assigns a fresh random id to every item on every
            # run; keep the old one so the id doesn't churn when nothing else changed.
            old_id = old_item.get("id")
            if old_id:
                merged["id"] = old_id
            synced_items.append(merged)
            changes["updated"].append(f"{key} ({folder_name})")
        else:
            synced_items.append(new_item)
            changes["added"].append(f"{key} ({folder_name})")

    for key, remaining in queues.items():
        for _ in remaining:
            changes["deleted"].append(f"{key} ({folder_name})")

    result = dict(new_folder)
    result["item"] = synced_items
    old_folder_events = old_folder.get("event")
    if old_folder_events:
        result["event"] = old_folder_events
    return result


def collect_path_variables(folder: dict) -> set[str]:
    keys: set[str] = set()
    for item in folder.get("item", []):
        url = item.get("request", {}).get("url", {})
        if isinstance(url, dict):
            for var in url.get("variable", []):
                k = var.get("key", "")
                if k:
                    keys.add(k)
    return keys


def sync_collection(base_path: str, existing_path: str, env_path: str):
    with open(base_path) as f:
        base: dict = json.load(f)
    with open(existing_path) as f:
        existing: dict = json.load(f)
    with open(env_path) as f:
        env: dict = json.load(f)

    for required, path in ((base, base_path), (existing, existing_path)):
        if "item" not in required:
            raise ValueError(f"{path}: missing top-level 'item' array — not a valid Postman collection")
    if "values" not in env:
        raise ValueError(f"{env_path}: missing top-level 'values' array — not a valid Postman environment")

    e2e_folders = [f for f in existing["item"] if f["name"].startswith(E2E_PREFIX)]
    old_resource: dict[str, dict] = {
        f["name"]: f for f in existing["item"] if not f["name"].startswith(E2E_PREFIX)
    }

    new_resource_folders = [f for f in base["item"] if not f["name"].startswith(E2E_PREFIX)]

    changes: dict[str, list] = {
        "added": [],
        "updated": [],
        "deleted": [],
        "folders_added": [],
        "folders_orphaned": [],
    }

    new_folder_names = {f["name"] for f in new_resource_folders}

    synced_folders = []
    for new_folder in new_resource_folders:
        folder_name = new_folder["name"]
        if folder_name not in old_resource:
            changes["folders_added"].append(folder_name)
        synced_folders.append(sync_folder(new_folder, old_resource.get(folder_name), changes))

    # Folders that used to be resource folders but no longer match any tag in the new
    # spec are kept as-is, not deleted — they may be a renamed/removed Tapir tag, or a
    # folder a human added by hand that has nothing to do with the generated spec.
    orphaned_folders = []
    for old_name, old_folder in old_resource.items():
        if old_name not in new_folder_names:
            changes["folders_orphaned"].append(old_name)
            orphaned_folders.append(old_folder)

    existing_env_keys = {v["key"] for v in env.get("values", [])}
    all_synced_folders = synced_folders + orphaned_folders + e2e_folders
    for folder in all_synced_folders:
        for var_key in collect_path_variables(folder):
            if var_key not in existing_env_keys:
                env["values"].append(
                    {"key": var_key, "value": "", "type": "default", "enabled": True}
                )
                existing_env_keys.add(var_key)

    # A variable can be referenced as a URL path segment (:code) or from inside a
    # pre-request/test script (pm.environment.get('code'), {{code}} templating, etc).
    # Scripts can't be reliably parsed, so fall back to a substring search over the
    # whole serialized collection — anywhere the key's name is quoted counts as a use.
    collection_text = json.dumps(all_synced_folders)
    changes["env_unused"] = sorted(
        k for k in existing_env_keys if k != "baseUrl" and k not in collection_text
    )

    synced = dict(existing)
    synced["item"] = synced_folders + orphaned_folders + e2e_folders

    return synced, env, changes


def print_report(changes: dict, existing_path: str, env_path: str) -> None:
    total = len(changes["added"]) + len(changes["updated"]) + len(changes["deleted"])
    folder_changes = len(changes["folders_added"]) + len(changes["folders_orphaned"])

    print()
    print("=" * 54)
    print("  Postman Collection Sync Report")
    print("=" * 54)

    for f in changes["folders_added"]:
        print(f"  + Folder added:    {f}")
    for f in changes["folders_orphaned"]:
        print(f"  ? Folder orphaned: {f}  (not in current spec, kept as-is — remove manually if obsolete)")
    for r in changes["added"]:
        print(f"  + Added:   {r}")
    for r in changes["updated"]:
        print(f"  ~ Updated: {r}")
    for r in changes["deleted"]:
        print(f"  - Deleted: {r}")

    if total == 0 and folder_changes == 0:
        print("  = No changes — collection is already up to date")

    print()
    print("  E2E folders preserved unchanged")

    if changes["env_unused"]:
        print()
        print("  Possibly unused environment variables (not removed automatically):")
        for k in changes["env_unused"]:
            print(f"    - {k}")

    print("=" * 54)
    print(f"  Written: {existing_path}")
    print(f"  Written: {env_path}")
    print()
    print("  Changes are in the working tree only — review with `git diff` and revert")
    print("  with `git checkout -- <file>` if anything looks wrong before committing.")
    print()


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: sync.py <base-collection.json> <existing-collection.json> <environment.json>")
        sys.exit(1)

    base_p, existing_p, env_p = sys.argv[1], sys.argv[2], sys.argv[3]

    synced, env, changes = sync_collection(base_p, existing_p, env_p)

    with open(existing_p, "w") as f:
        json.dump(synced, f, indent=2, ensure_ascii=False)
        f.write("\n")
    with open(env_p, "w") as f:
        json.dump(env, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print_report(changes, existing_p, env_p)
