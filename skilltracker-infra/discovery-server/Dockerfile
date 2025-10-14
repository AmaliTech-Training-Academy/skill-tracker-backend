FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml ./pom.xml

# Copy common modules
COPY skilltracker-common ./skilltracker-common

# Copy this service's pom
COPY skilltracker-infra/discovery-server/pom.xml ./skilltracker-infra/discovery-server/pom.xml

# Download dependencies (cached layer)
RUN cd skilltracker-infra/discovery-server && mvn dependency:go-offline -B

# Copy source
COPY skilltracker-infra/discovery-server/src ./skilltracker-infra/discovery-server/src

# Build
RUN cd skilltracker-infra/discovery-server && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/skilltracker-infra/discovery-server/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]