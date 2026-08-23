FROM node:24-alpine AS frontend-development
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

FROM frontend-development AS frontend-build
RUN npm run build

FROM maven:3.9-eclipse-temurin-21-alpine AS backend-development
WORKDIR /app
COPY backend/pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -q
COPY backend/src src
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]

FROM backend-development AS backend-build
COPY --from=frontend-build /app/build src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S exptrack && adduser -S -G exptrack exptrack && mkdir /data && chown exptrack:exptrack /data
COPY --from=backend-build --chown=exptrack:exptrack /app/target/backend-*.jar app.jar
ENV EXPTRACK_DATABASE_PATH=/data/exptrack.db
VOLUME /data
EXPOSE 8080
USER exptrack
ENTRYPOINT ["java", "-jar", "app.jar"]
