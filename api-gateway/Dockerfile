# jdk 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder
# Set the working directory
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src
# Build the application

RUN mvn clean package

FROM openjdk:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar
# Expose the port the app runs on
EXPOSE 4004

ENTRYPOINT ["java", "-jar", "app.jar"]