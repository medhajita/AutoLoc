FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Couche de dépendances mise en cache : un changement de code ne retélécharge plus Maven Central
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
# SerialGC + mémoire bornée au conteneur : démarrage plus rapide, moins de RAM, pas de pauses de threads GC
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70", "-Xss512k", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
