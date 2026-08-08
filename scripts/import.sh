#!/usr/bin/env bash
#
# import.sh - create matched Reddit datasets via the local reddit-intelligence API.
#
# Resumable: skips any (subreddit, query, sort, timeRange) combination that is
# already COMPLETED, so re-running after a failure costs no extra Crawlora credits.
#
#   ./import.sh            # run the matrix
#   ./import.sh --dry-run  # show what would run, spend nothing
#
set -euo pipefail

API="${API:-http://localhost:8080}"
SORT="${SORT:-relevance}"
TIME_RANGE="${TIME_RANGE:-year}"
MAX_POSTS="${MAX_POSTS:-50}"
DRY_RUN=0
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

# ---------------------------------------------------------------------------
# The matrix. Identical queries on both sides - that is the whole point.
# Tier 1 only by default. Uncomment tier 2 once you have seen tier 1 output.
# ---------------------------------------------------------------------------
MATRIX=(
  # --- Tier 1: migrant/visa subs. Highest signal. ---
  "NewToNewZealand|international student job"
  "AusVisa|international student job"
  "NewToNewZealand|graduate job market"
  "AusVisa|graduate job market"

  # --- Tier 2: country-general mood. Uncomment when ready. ---
  # "newzealand|international student job"
  # "australian|international student job"
  # "newzealand|graduate job market"
  # "australian|graduate job market"

  # --- Tier 3: course texture. Weakest signal, spend last. ---
  # "universityofauckland|Masters in IT"
  # "UniversityOfAdelaide|Master of Information Technology"
)

for c in curl jq; do
  command -v "$c" >/dev/null || { echo "error: $c not found on PATH" >&2; exit 1; }
done

curl -sf "${API}/api/datasets" >/dev/null 2>&1 || {
  echo "error: cannot reach ${API}. Is Quarkus running (./gradlew quarkusDev)?" >&2
  exit 1
}

existing="$(curl -sf "${API}/api/datasets")"

printf '%-28s %-32s %s\n' "SUBREDDIT" "QUERY" "RESULT"
printf '%.0s-' {1..90}; echo

for row in "${MATRIX[@]}"; do
  subreddit="${row%%|*}"
  query="${row#*|}"

  already="$(jq -r --arg s "$subreddit" --arg q "$query" --arg so "$SORT" --arg t "$TIME_RANGE" '
    [ .[] | select(.subreddit==$s and .query==$q and .sort==$so
                   and .timeRange==$t and .status=="COMPLETED") ] | first // empty
  ' <<<"$existing")"

  if [[ -n "$already" ]]; then
    id="$(jq -r '.datasetId' <<<"$already")"
    n="$(jq -r '"\(.postsImported)p/\(.commentsImported)c"' <<<"$already")"
    printf '%-28s %-32s SKIP (dataset %s, %s)\n' "$subreddit" "$query" "$id" "$n"
    continue
  fi

  if (( DRY_RUN )); then
    printf '%-28s %-32s WOULD RUN\n' "$subreddit" "$query"
    continue
  fi

  body="$(jq -n --arg s "$subreddit" --arg q "$query" --arg so "$SORT" \
                --arg t "$TIME_RANGE" --argjson m "$MAX_POSTS" \
    '{subreddit:$s, query:$q, sort:$so, timeRange:$t, maxPosts:$m, includeComments:true}')"

  printf '%-28s %-32s running...' "$subreddit" "$query"

  if resp="$(curl -sS --fail-with-body -X POST "${API}/api/datasets" \
              -H 'Content-Type: application/json' -d "$body" 2>&1)"; then
    printf '\r%-28s %-32s %s\n' "$subreddit" "$query" \
      "$(jq -r '"OK  dataset \(.datasetId)  \(.postsImported)p/\(.commentsImported)c"' <<<"$resp")"
    existing="$(curl -sf "${API}/api/datasets")"
  else
    printf '\r%-28s %-32s FAILED: %s\n' "$subreddit" "$query" \
      "$(head -c 200 <<<"$resp" | tr -d '\n')"
  fi
done

echo
echo "Done. Now run ./export.sh"