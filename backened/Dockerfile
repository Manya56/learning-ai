# Stage 1 — Build
FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2 — Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# ✅ THIS IS THE FIX
COPY backened/models ./models

EXPOSE 10000

ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
