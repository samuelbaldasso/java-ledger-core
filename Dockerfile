# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the application jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal, secure runtime image
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Run as non-root user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

COPY --from=builder /build/target/ledger-core-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
