# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY order-service/pom.xml order-service/
COPY inventory-service/pom.xml inventory-service/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl order-service -am dependency:go-offline
COPY order-service/src order-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl order-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
