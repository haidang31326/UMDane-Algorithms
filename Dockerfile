# Stage 1: Build the Spring Boot application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Install Docker CLI to support Docker-out-of-Docker (DooD) sandbox
RUN apk add --no-cache docker-cli
COPY --from=build /app/target/UMDane-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
