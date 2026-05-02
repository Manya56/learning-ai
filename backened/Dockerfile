# Stage 1 — Build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2 — Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# models/ folder no longer needed — embedding runs as separate service
# DO NOT copy models here anymore

EXPOSE 10000

ENTRYPOINT ["java", \
  "-Xmx600m", \
  "-Xms200m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=80.0", \
  "-Djava.awt.headless=true", \
  "-jar", "app.jar"]