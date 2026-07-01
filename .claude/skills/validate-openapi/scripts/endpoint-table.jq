.paths | to_entries[] | .key as $path |
  .value | to_entries[] |
  select(.value | type == "object") |
  .key as $method | .value |
  [(.tags[0] // "untagged"), ($method | ascii_upcase), $path] | @tsv
