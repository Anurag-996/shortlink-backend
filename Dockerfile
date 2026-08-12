# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-25 AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --chown=spring:spring --from=builder /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]