# Multi-stage build: compile in Maven container, run in smaller JVM container
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src/ ./src/
RUN mvn clean package -DskipTests

# Runtime stage: use minimal JRE image
FROM eclipse-temurin:21-jre-focal
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/done-yet-*.jar app.jar

# Expose port (Fly.io will inject $PORT env var)
EXPOSE 8080

# Set default port (will be overridden by Fly.io's $PORT env var)
ENV PORT=8080

# Enable container support for JVM memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport"

# Start application
CMD ["java", "-jar", "app.jar"]
