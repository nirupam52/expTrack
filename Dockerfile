FROM node:24-alpine@sha256:d32cdf619f63fe0471182d08996dd516c6275bb5fd31ae06e55a570bd9e1ad43 AS frontend-development
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

FROM frontend-development AS frontend-build
RUN npm run build

FROM maven:3.9-eclipse-temurin-21-alpine@sha256:65353f527c86cb23187c8233475713e15067e8d36220d18863c379680698fe85 AS backend-development
WORKDIR /app
COPY backend/pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -q
COPY backend/src src
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]

FROM backend-development AS backend-build
COPY --from=frontend-build /app/build src/main/resources/static
# Reuse Maven downloads without adding them to the image.
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699
WORKDIR /app
# Keep a stable UID for the mounted SQLite volume.
RUN addgroup -S -g 10001 exptrack \
	&& adduser -S -D -H -u 10001 -G exptrack exptrack \
	&& mkdir /data \
	&& chown exptrack:exptrack /data
COPY --from=backend-build --chown=exptrack:exptrack /app/target/backend-*.jar app.jar
ENV EXPTRACK_DATABASE_PATH=/data/exptrack.db
EXPOSE 8080
# Run the application without root privileges.
USER exptrack
CMD ["java", "-jar", "app.jar"]
