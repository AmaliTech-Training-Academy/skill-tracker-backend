FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom
COPY pom.xml ./pom.xml

# Copy common modules
COPY skilltracker-common ./skilltracker-common

# Copy this service's pom
COPY skilltracker-infra/api-gateway/pom.xml ./skilltracker-infra/api-gateway/pom.xml

# Download dependencies (cached layer)
RUN cd skilltracker-infra/api-gateway && mvn dependency:go-offline -B

# Copy source
COPY skilltracker-infra/api-gateway/src ./skilltracker-infra/api-gateway/src

# Build
RUN cd skilltracker-infra/api-gateway && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/skilltracker-infra/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]