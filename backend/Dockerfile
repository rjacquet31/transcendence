# --- Etape 1 : compile le projet en jar executable ---
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app

# Dependances d'abord : ce layer reste en cache tant que pom.xml ne change pas
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests \
    && cp target/*.jar app.jar

# --- Etape 2 : image d'execution, juste Java + le jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/app.jar app.jar

# Dossier des avatars uploades (app.avatar.storage-dir), a monter en volume
RUN mkdir -p uploads/avatars

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
