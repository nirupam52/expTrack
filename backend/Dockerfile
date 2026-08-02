FROM maven:3.9-eclipse-temurin-21-alpine AS development
WORKDIR /app

FROM development AS build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -q
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-*.jar app.jar
ENV EXPTRACK_DATABASE_PATH=/data/exptrack.db
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
