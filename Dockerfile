FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Installation de Maven
RUN apt-get update && apt-get install -y maven

# Optimisation: copier d'abord le POM pour télécharger les dépendances
COPY pom.xml .
# Télécharger toutes les dépendances et les mettre en cache
RUN mvn dependency:go-offline -B

# Ensuite copier le code source
COPY src ./src/

# Nettoyer le répertoire local Maven pour économiser de l'espace
RUN mvn clean package -DskipTests && \
    rm -rf ~/.m2/repository/

# Utiliser une image de base disponible au lieu de eclipse-temurin:17-jre-alpine
FROM eclipse-temurin:17-jre
WORKDIR /app
<<<<<<< HEAD
COPY --from=build /app/target/RECETTE-0.0.1-SNAPSHOT.jar app.jar
=======
COPY --from=build /app/target/*.jar app.jar
>>>>>>> 56b93080ce58e01587c4613533cc788226e890d7
EXPOSE 8080

# Options JVM pour optimiser l'utilisation de la mémoire
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]