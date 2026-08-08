# reddit-intelligence

`reddit-intelligence` is a Java 25 / Quarkus backend that creates research datasets from Reddit data returned by [Crawlora's Reddit APIs](https://crawlora.net/docs/reddit). A dataset records one subreddit search request, stores matching posts, rebuilds and stores the returned comment hierarchy, and exposes the collected raw data through REST APIs.

This repository is Phase 1. It intentionally contains no LLMs, embeddings, vector storage, RAG, sentiment processing, authentication, or UI.

## Architecture

The API performs imports synchronously: `POST /api/datasets` returns after the Crawlora-backed Reddit import has finished or failed. The components are deliberately small:

- `api`: validated request records, response records, and REST resources.
- `dataset`: dataset orchestration and import statistics.
- `reddit`: replaceable `RedditClient`, Crawlora REST client, retry/cursor pagination logic, normalized transport records, flat-comment hierarchy reconstruction, and importer.
- `persistence`: Panache entities and transaction-scoped persistence operations.
- `post` and `comment`: read services that map entities to API records.
- `config`: typed Crawlora configuration.
- Flyway owns the production schema; Hibernate only validates it.

`RedditClient` keeps the collection provider separate from dataset orchestration. Crawlora JSON objects are parsed into transport records and never used as persistence entities. Persistence entities are never returned directly from the REST API.

## Requirements

- JDK 25
- Docker Desktop, Docker Engine, or a compatible container runtime
- Docker Compose v2
- A Crawlora API key

The Gradle wrapper is included; a system Gradle installation is not required. The project uses Quarkus 3.38.1, PostgreSQL 16, Hibernate ORM with Panache, Flyway, Quarkus REST, Quarkus REST Client/Jackson, Bean Validation, JUnit, Mockito, and Quarkus PostgreSQL Dev Services backed by Testcontainers.

## Start PostgreSQL

From the project root:

```bash
docker compose up -d postgres
docker compose ps
```

The database is exposed at `localhost:5432` with:

- database: `reddit_intelligence`
- username: `reddit`
- password: `reddit`

The Compose service includes a PostgreSQL health check and a named volume. Stop it with `docker compose down`. Add `-v` only when you intentionally want to delete all local database data.

## Run the application

Confirm that `java -version` reports Java 25, set a server-side Crawlora key, then run:

```bash
export CRAWLORA_API_KEY="replace-with-your-rotated-key"
./gradlew quarkusDev
```

If a key has appeared in a pasted request, terminal capture, or source file, rotate it in the Crawlora console before using it here. The application can start without a key, but dataset creation fails with a clear configuration error until `CRAWLORA_API_KEY` is set.

The API listens on `http://localhost:8080`. Swagger UI is available in development mode at `http://localhost:8080/q/swagger-ui`.

For a packaged build:

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

Database settings can be overridden with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Crawlora settings in `application.properties` can be overridden with:

| Environment variable | Default |
| --- | --- |
| `CRAWLORA_API_KEY` | none; required for imports |
| `CRAWLORA_BASE_URL` | `https://api.crawlora.net` |
| `CRAWLORA_CONNECT_TIMEOUT` | `5000` ms |
| `CRAWLORA_READ_TIMEOUT` | `30000` ms |
| `CRAWLORA_MAX_RETRIES` | `3` |
| `CRAWLORA_RETRY_DELAY` | `1000` ms base exponential delay |
| `CRAWLORA_RATE_LIMIT_RETRY_DELAY` | `60000` ms when a 429 has no `Retry-After` |
| `CRAWLORA_COMMENTS_LIMIT` | `100` (valid range `1`–`100`) |

HTTP 429 and 5xx responses, plus transport failures, are retried with bounded exponential backoff. A numeric `Retry-After` header takes precedence. Authentication and other non-retryable 4xx responses fail immediately.

## Run from IntelliJ IDEA

The repository includes a shared `Quarkus Dev` Gradle run configuration. Open the repository root as the IntelliJ project, edit that run configuration once, and add `CRAWLORA_API_KEY` under environment variables. Keep the value local; do not save the secret into the shared XML file.

Start PostgreSQL first, then select **Quarkus Dev** from the run configuration menu. A non-secret environment template is available in `.env.example` for reference.

## First University of Auckland import

With PostgreSQL healthy and Quarkus running, execute:

```bash
curl --fail-with-body -X POST http://localhost:8080/api/datasets \
  -H 'Content-Type: application/json' \
  -d '{
    "subreddit": "universityofauckland",
    "query": "Master of Artificial Intelligence",
    "sort": "relevance",
    "timeRange": "all",
    "maxPosts": 100,
    "includeComments": true
  }'
```

A successful response resembles:

```json
{
  "datasetId": 1,
  "subreddit": "universityofauckland",
  "query": "Master of Artificial Intelligence",
  "sort": "relevance",
  "timeRange": "all",
  "maxPosts": 100,
  "includeComments": true,
  "postsImported": 12,
  "commentsImported": 87,
  "status": "COMPLETED",
  "createdAt": "2026-08-08T00:00:00Z",
  "completedAt": "2026-08-08T00:00:05Z",
  "errorMessage": null
}
```

## Inspect collected data

```bash
curl http://localhost:8080/api/datasets
curl http://localhost:8080/api/datasets/1
curl http://localhost:8080/api/datasets/1/posts
curl http://localhost:8080/api/posts/1
curl http://localhost:8080/api/posts/1/comments
curl http://localhost:8080/api/comments/1
```

`postId` and `commentId` in API paths are local database IDs; every response also includes the original Reddit ID. Post comments are returned as a flat list with `parentCommentId` and `depth`, which preserves and exposes the hierarchy without duplicating nested JSON.

## Run tests

Docker must be running. Normal tests mock the provider boundary and never call live Crawlora or spend credits:

```bash
./gradlew test
```

Quarkus Dev Services starts a disposable PostgreSQL 16 Testcontainer, Flyway migrates it, and the integration test exercises dataset creation, post persistence, nested comment persistence, hierarchy, and a repeated import without raw-data duplicates. Crawlora client and parser tests use fixtures in `src/test/resources/reddit/` and cover normalized fields, missing fields, flat parent links, deleted comments, cursor pagination, incomplete comment pages, retryable failures, and non-retryable authentication errors.

Run the complete verification and package the service with:

```bash
./gradlew build
```

## Database model and deduplication

- `dataset`: the complete import request and its lifecycle/statistics.
- `reddit_post`: raw post metadata. `reddit_id` has a database unique constraint.
- `reddit_comment`: raw comment metadata and a self-referencing `parent_comment_id`. `reddit_id` has a database unique constraint.
- `dataset_post`: many-to-many link between a research dataset and deduplicated posts, with a composite primary key.

Re-importing the same search creates a new dataset, refreshes the matching raw post/comment metadata, and links the new dataset to existing posts. Unique constraints provide the final database-level duplicate guarantee.

`comments_downloaded=true` means the Crawlora comments endpoint returned and its represented comments were stored. `comments_complete=true` means fewer comments than the configured provider limit were returned and the parsed count was at least the post's reported comment count. It means “appears complete,” not a guarantee that Reddit exposed every historical comment. A response containing exactly the configured limit is marked incomplete because Crawlora's comments endpoint has no continuation cursor.

## Crawlora collection characteristics

The service calls `GET /api/v1/reddit/search` and `GET /api/v1/reddit/comments/{id}`. Search results are cursor-paginated in pages of at most 100 until `maxPosts` is reached, results end, a cursor repeats, or a page adds no new posts. Each successful search page and comment request consumes Crawlora credits according to the active plan.

Crawlora's default public comment response is flat and currently clamps `limit` to 100. The parser uses `parent_id` when present to rebuild nested records; a comment whose parent was not returned is retained as a root so collected data is not discarded. Missing scores and reported comment counts are stored as zero. Deleted/removed bodies are retained as returned.

The client does not log the API key or full response bodies. A comment failure leaves `comments_downloaded=false` for a new post and allows other posts in the dataset to be retained. A search-level failure marks the dataset `FAILED` with an error message.

## Future roadmap

Phase 2:

- topic extraction
- claim extraction
- evidence aggregation
- sentiment
- LLM-generated decision reports

Phase 3:

- embeddings
- pgvector
- semantic search
- RAG / conversational exploration
