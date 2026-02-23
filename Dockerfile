FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon -q

COPY src/ src/
RUN ./gradlew bootJar -x test --no-daemon -q

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd -g 999 docker-host || true

COPY --from=builder /app/build/libs/smu-worker-api-*.jar app.jar

EXPOSE ${WORKER_PORT:-8081}

ENTRYPOINT ["java", "-jar", "app.jar"]
