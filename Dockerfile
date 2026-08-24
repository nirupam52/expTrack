FROM node:24.19.0-alpine3.24 AS frontend-development
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

FROM frontend-development AS frontend-build
RUN npm run build

FROM maven:3.9.16-eclipse-temurin-21-alpine AS backend-development
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

FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24
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
