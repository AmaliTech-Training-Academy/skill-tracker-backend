FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

COPY skilltracker-services/analytics-service/pom.xml skilltracker-services/analytics-service/
COPY skilltracker-services/analytics-service/src skilltracker-services/analytics-service/src

COPY skilltracker-common skilltracker-common
COPY skilltracker-infra skilltracker-infra

WORKDIR /app/skilltracker-services/analytics-service/

RUN mvn clean package -DskipTests

WORKDIR /app

# -------------------------------------------------------
# RUNTIME STAGE
# -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/skilltracker-services/analytics-service/target/*.jar app.jar

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