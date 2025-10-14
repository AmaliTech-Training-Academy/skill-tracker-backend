# -------------------------------------------------------
# Skill Tracker Backend - User Service Dockerfile
# -------------------------------------------------------

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

COPY skilltracker-services/user-service/pom.xml skilltracker-services/user-service/
COPY skilltracker-services/user-service/src skilltracker-services/user-service/src

COPY skilltracker-common skilltracker-common
COPY skilltracker-infra skilltracker-infra

WORKDIR /app/skilltracker-services/user-service/

RUN mvn clean package -DskipTests

WORKDIR /app

# -------------------------------------------------------
# RUNTIME STAGE
# -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/skilltracker-services/user-service/target/*.jar app.jar

EXPOSE 8081

# -------------------------------------------------------
# Run Application
# -------------------------------------------------------
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
