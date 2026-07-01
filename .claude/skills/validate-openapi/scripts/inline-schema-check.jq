.paths | to_entries[] | .key as $path |
  .value | to_entries[] |
  select(.value | type == "object") |
  .key as $method | .value |
  (
    (.requestBody?.content // {}) | to_entries[] | .key as $mt | .value.schema |
    select(. != null and (."$ref" == null) and (.type == "object" or .properties != null)) |
    "  REQUEST  \($method|ascii_upcase) \($path) [\($mt)] — inline object schema"
  ),
  (
    (.responses // {}) | to_entries[] | .key as $status |
    .value.content? // {} | to_entries[] | .key as $mt | .value.schema |
    select(. != null) |
    (
      select(."$ref" == null and (.type == "object" or .properties != null)) |
      "  RESPONSE \($method|ascii_upcase) \($path) [\($status)] [\($mt)] — inline object schema"
    ),
    (
      select(.type == "array" and .items != null and
             (.items."$ref" == null) and (.items.type == "object" or .items.properties != null)) |
      "  RESPONSE \($method|ascii_upcase) \($path) [\($status)] [\($mt)] — inline object in array items"
    )
  )
