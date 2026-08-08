#!/usr/bin/env bash
#
# export.sh - dump every COMPLETED dataset to datasets/<country>/<slug>.json
#             and write datasets/manifest.json.
#
# Strips the `author` field from posts and comments before writing: the export
# is committed to a public repo and usernames add nothing to the analysis.
#
# Comment data is streamed through temp files rather than shell variables, so
# datasets with thousands of comments do not hit ARG_MAX.
#
#   ./export.sh              # export all COMPLETED datasets
#   ./export.sh 154 157      # export only these dataset ids
#   FORCE=1 ./export.sh      # re-export files that already exist
#
set -euo pipefail

API="${API:-http://localhost:8080}"
OUT="${OUT:-datasets}"
FORCE="${FORCE:-0}"

for c in curl jq; do
  command -v "$c" >/dev/null || { echo "error: $c not found on PATH" >&2; exit 1; }
done

curl -sf "${API}/api/datasets" >/dev/null 2>&1 || {
  echo "error: cannot reach ${API}. Is Quarkus running?" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# subreddit -> country bucket. Add new subreddits here.
country_of() {
  case "$(tr '[:upper:]' '[:lower:]' <<<"$1")" in
    newtonewzealand|newzealand|auckland|personalfinancenz|universityofauckland|\
    waikato|christchurch|wellington|canterbury) echo nz ;;
    ausvisa|australian|australia|adelaide|ausfinance|auscorp|\
    universityofadelaide|melbourne|sydney|perth) echo au ;;
    *) echo other ;;
  esac
}

slugify() {
  tr '[:upper:]' '[:lower:]' <<<"$1" | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//'
}

mkdir -p "$OUT"
curl -sf "${API}/api/datasets" > "${TMP}/all.json"

if (( $# > 0 )); then
  printf '%s\n' "$@" > "${TMP}/ids"
else
  jq -r '.[] | select(.status=="COMPLETED") | .datasetId' "${TMP}/all.json" > "${TMP}/ids"
fi

[[ -s "${TMP}/ids" ]] || { echo "No COMPLETED datasets found."; exit 0; }

: > "${TMP}/manifest.ndjson"

while read -r id; do
  [[ -n "$id" ]] || continue

  if ! curl -sS --fail-with-body "${API}/api/datasets/${id}" > "${TMP}/meta.json"; then
    echo "  dataset ${id}: FAILED to fetch metadata, skipping" >&2; continue
  fi

  subreddit="$(jq -r '.subreddit' "${TMP}/meta.json")"
  country="$(country_of "$subreddit")"
  slug="$(slugify "${subreddit}__$(jq -r '.query' "${TMP}/meta.json")")"
  slug="${slug:0:70}"; slug="${slug%-}"
  file="${country}/${slug}--ds${id}.json"

  if [[ -f "${OUT}/${file}" && "$FORCE" != "1" ]]; then
    echo "dataset ${id}  r/${subreddit}  (already exported, skipping)"
  else
    echo "dataset ${id}  r/${subreddit}"

    if ! curl -sS --fail-with-body "${API}/api/datasets/${id}/posts" > "${TMP}/posts.json"; then
      echo "  FAILED to fetch posts, skipping" >&2; continue
    fi

    n="$(jq 'length' "${TMP}/posts.json")"
    : > "${TMP}/comments.ndjson"
    i=0

    while read -r pid; do
      [[ -n "$pid" ]] || continue
      if curl -sS --fail-with-body "${API}/api/posts/${pid}/comments" > "${TMP}/pc.json" 2>/dev/null; then
        jq -c 'if type=="array" then .[] else empty end' "${TMP}/pc.json" \
          >> "${TMP}/comments.ndjson" 2>/dev/null \
          || echo "  warn: unparseable comments for post ${pid}" >&2
      else
        echo "  warn: comments fetch failed for post ${pid}" >&2
      fi
      i=$((i+1)); printf '\r  posts %d/%d' "$i" "$n"
    done < <(jq -r '.[].id' "${TMP}/posts.json")
    printf '\r                              \r'

    mkdir -p "${OUT}/${country}"

    # --slurpfile keeps payloads off the command line: no ARG_MAX limit.
    jq -n \
      --slurpfile d "${TMP}/meta.json" \
      --slurpfile p "${TMP}/posts.json" \
      --slurpfile c "${TMP}/comments.ndjson" '
      {
        dataset:  $d[0],
        posts:    ($p[0] | map(del(.author))),
        comments: ($c    | map(del(.author)) | unique_by(.redditId))
      }' > "${OUT}/${file}"
  fi

  n_posts="$(jq '.posts | length' "${OUT}/${file}")"
  n_comments="$(jq '.comments | length' "${OUT}/${file}")"
  truncated="$(jq '[.posts[] | select(.commentsComplete == false)] | length' "${OUT}/${file}")"
  echo "  -> ${OUT}/${file}  (${n_posts}p, ${n_comments}c, ${truncated} truncated)"

  jq -n --slurpfile d "${TMP}/meta.json" \
        --arg file "$file" --arg country "$country" \
        --argjson posts "$n_posts" --argjson comments "$n_comments" \
        --argjson truncated "$truncated" -c '
    {
      file: $file, country: $country,
      subreddit: $d[0].subreddit, query: $d[0].query,
      sort: $d[0].sort, timeRange: $d[0].timeRange, fromDate: $d[0].fromDate,
      maxPosts: $d[0].maxPosts, datasetId: $d[0].datasetId,
      postsImported: $posts, commentsImported: $comments,
      truncatedThreads: $truncated, importedAt: $d[0].completedAt,
      subscribers: null
    }' >> "${TMP}/manifest.ndjson"
done < "${TMP}/ids"

jq -s '
  {
    generatedAt: (now | todateiso8601),
    note: "author field stripped. subscribers must be filled in by hand - the API does not expose it.",
    datasets: sort_by(.country, .subreddit, .query)
  }' "${TMP}/manifest.ndjson" > "${OUT}/manifest.json"

echo
echo "Wrote ${OUT}/manifest.json with $(jq '.datasets | length' "${OUT}/manifest.json") datasets."
echo "TODO: fill in the null \"subscribers\" values before committing."