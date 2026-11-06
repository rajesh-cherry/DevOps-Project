# Build from repository root: docker build -f docker/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY application/pom.xml ./application/pom.xml
COPY application/src ./application/src
RUN mvn -f application/pom.xml -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/application/target/employee-api-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
