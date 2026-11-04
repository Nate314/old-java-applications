# syntax=docker/dockerfile:1
# The web arcade in a container, for docker compose (docker-compose.yml, started by ./run.ps1).
# Stage 1 builds the site exactly as `mvn -f web/pom.xml package` does on the host; stage 2 serves it with nginx.

FROM node:22-bookworm-slim AS node

# --- Stage 1: build web/target/site (TeaVM apps, the React pages, the jars) ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
# git: the build reads the repo's history (web/then's first restored sources, What's new). Node: web/react.
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm
WORKDIR /repo
# The whole checkout, .git included (see .dockerignore for what is left out).
COPY . .
RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.npm \
    mvn -B -q -f web/pom.xml package

# --- Stage 2: serve the static site ---
FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime
COPY web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=101:101 /repo/web/target/site /usr/share/nginx/html
EXPOSE 8080
USER 101
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO /dev/null http://127.0.0.1:8080/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
