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

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

COPY --from=builder /workspace/target/bss-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
