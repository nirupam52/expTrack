# ExpTrack

## Run the app

Install Docker Desktop, then run:

```sh
docker compose up --build
```

Open http://localhost:8080. Data persists in Docker's `postgres-data` volume.

Local database credentials default to `exptrack` for the database name, user, and password. Override them before starting the stack with the `EXPTRACK_DATABASE_NAME`, `EXPTRACK_DATABASE_USERNAME`, and `EXPTRACK_DATABASE_PASSWORD` environment variables.

To reset to an empty database, stop the stack and remove its volumes:

```sh
docker compose down --volumes
```

## Develop

```sh
docker compose -f compose.yaml -f compose.debug.yaml up --watch
```

Open http://localhost:5173 for the Svelte dev server. The Spring Boot API remains at http://localhost:8080, and PostgreSQL is reachable at `127.0.0.1:5432` for direct inspection.
