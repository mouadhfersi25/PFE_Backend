FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# JAR produit par ./mvnw package (stage Jenkins Build)
COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
