# Use Java 17 JRE base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the Spring Boot JAR
COPY target/delay-prediction-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]