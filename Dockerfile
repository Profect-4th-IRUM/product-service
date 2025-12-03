# syntax=docker/dockerfile:1.4

FROM gradle:8.7-jdk21 AS build

WORKDIR /app

ARG GIT_USERNAME
ARG GIT_TOKEN

ENV GIT_USERNAME=${GIT_USERNAME}
ENV GIT_TOKEN=${GIT_TOKEN}

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

COPY . .
RUN ./gradlew clean bootJar --no-daemon --build-cache

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# healthCheck를 위해 curl 설치
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

RUN curl -L \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar \
  -o /app/opentelemetry-javaagent.jar

COPY --from=build /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul

ENTRYPOINT ["java","-javaagent:/app/opentelemetry-javaagent.jar","-Dotel.service.name=product-service","-Dotel.propagators=tracecontext,baggage,b3,b3multi","-Dotel.traces.exporter=otlp","-Dotel.logs.exporter=none","-Dotel.metrics.exporter=none","-Dotel.exporter.otlp.endpoint=http://otel-collector.istio-system.svc.cluster.local:4317","-Dotel.exporter.otlp.protocol=grpc","-jar", "/app/app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
