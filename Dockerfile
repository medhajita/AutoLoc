# 1) Sites React : /client/ et /admin/ (build Vite)
FROM node:22-alpine AS front-client
WORKDIR /w
COPY frontend/client/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/client ./
RUN npm run build

FROM node:22-alpine AS front-admin
WORKDIR /w
COPY frontend/admin/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/admin ./
RUN npm run build

# 2) API Spring Boot : les deux builds sont embarqués dans le jar (static/client, static/admin)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Couche de dépendances mise en cache : un changement de code ne retélécharge plus Maven Central
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY --from=front-client /w/dist ./src/main/resources/static/client
COPY --from=front-admin /w/dist ./src/main/resources/static/admin
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
# SerialGC + mémoire bornée au conteneur : démarrage plus rapide, moins de RAM, pas de pauses de threads GC
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70", "-Xss512k", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
