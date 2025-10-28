# syntax=docker/dockerfile:1.6

ARG BUILDER_IMAGE=eclipse-temurin:21-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${BUILDER_IMAGE} AS builder

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml pom.xml

RUN chmod +x mvnw

# Pre-fetch dependencies for caching
RUN ./mvnw -B -q dependency:go-offline

COPY src src

RUN ./mvnw -B -DskipTests=true clean package

FROM ${RUNTIME_IMAGE} AS runtime

ENV TZ=UTC

WORKDIR /app

COPY --from=builder /workspace/target/bss-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
