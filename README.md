# reddit-intelligence

`reddit-intelligence` is a Java 25 / Quarkus backend that creates research datasets from Reddit's public JSON endpoints. A dataset records one subreddit search request, stores matching posts, recursively stores the comments returned for each post, and exposes the collected raw data through REST APIs.

This repository is Phase 1. It intentionally contains no LLMs, embeddings, vector storage, RAG, sentiment processing, authentication, or UI.

## Architecture

The API performs imports synchronously: `POST /api/datasets` returns after the anonymous Reddit import has finished or failed. The components are deliberately small:

- `api`: validated request records, response records, and REST resources.
- `dataset`: dataset orchestration and import statistics.
- `reddit`: replaceable `RedditClient`, anonymous JSON REST client, retry/pagination logic, Reddit transport records, recursive parser, and importer.
- `persistence`: Panache entities and transaction-scoped persistence operations.
- `post` and `comment`: read services that map entities to API records.
- `config`: typed Reddit configuration.
- Flyway owns the production schema; Hibernate only validates it.

`RedditClient` is the boundary for a future OAuth/API implementation. Reddit JSON objects are parsed into transport records and never used as persistence entities. Persistence entities are never returned directly from the REST API.

## Requirements

- JDK 25
- Docker Desktop, Docker Engine, or a compatible container runtime
- Docker Compose v2

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

Confirm that `java -version` reports Java 25, then run:

```bash
./gradlew quarkusDev
```

The API listens on `http://localhost:8080`. Swagger UI is available in development mode at `http://localhost:8080/q/swagger-ui`.

For a packaged build:

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

Database settings can be overridden with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Reddit settings in `application.properties` can be overridden with:

| Environment variable | Default |
| --- | --- |
| `REDDIT_BASE_URL` | `https://www.reddit.com` |
| `REDDIT_USER_AGENT` | `reddit-intelligence/1.0 (contact: local-research)` |
| `REDDIT_CONNECT_TIMEOUT` | `5000` ms |
| `REDDIT_READ_TIMEOUT` | `30000` ms |
| `REDDIT_MAX_RETRIES` | `3` |
| `REDDIT_RETRY_DELAY` | `1000` ms |

For sustained use, set `REDDIT_USER_AGENT` to a descriptive value with a real contact or project reference.

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

Docker must be running. Normal tests never call live Reddit:

```bash
./gradlew test
```

Quarkus Dev Services starts a disposable PostgreSQL 16 Testcontainer, Flyway migrates it, and the integration test exercises dataset creation, post persistence, nested comment persistence, hierarchy, and a repeated import without raw-data duplicates. JSON parser tests use fixtures in `src/test/resources/reddit/` and cover missing fields, deleted comments, nested replies, unsupported kinds, and `more` objects.

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

`comments_downloaded=true` means the comments endpoint returned and its represented comments were stored. `comments_complete=true` means the response contained no `more` placeholders and the parsed count was at least Reddit's reported comment count. It means “appears complete,” not a guarantee that Reddit exposed every historical comment.

## Anonymous Reddit JSON limitations

Phase 1 uses Reddit's unauthenticated JSON endpoints, not HTML scraping. These endpoints can be rate-limited, return HTTP 403/429, change behavior, cap search results, omit removed/private/moderated content, and represent undisclosed comments with `more` placeholders. Search is limited by Reddit's own indexing and pagination behavior. Deleted authors can be null or `[deleted]`; deleted/removed bodies are retained as returned.

The client sends a proper User-Agent, follows search cursors, retries transport errors, HTTP 429, and server errors with bounded delays, and logs failures without logging full Reddit response bodies. A comment failure leaves `comments_downloaded=false` for a new post and allows other posts in the dataset to be retained. A search-level failure marks the dataset `FAILED` with an error message.

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
