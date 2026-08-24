# ExpTrack

## Run the app

Install Docker Desktop, then run:

```sh
docker compose up --build
```

Open http://localhost:8080. Data persists in Docker's `expense-data` volume.

The production service listens on the host loopback interface only. Run a TLS reverse proxy on the same host to provide public HTTPS access.

The app runs as the unprivileged `exptrack` user. Existing `expense-data` volumes must be owned by user and group `10001`.

## Develop

```sh
docker compose -f compose.yaml -f compose.debug.yaml up --watch
```

Open http://localhost:5173 for the Svelte dev server. The Spring Boot API remains at http://localhost:8080.
