
# Etapa 1: Build
FROM amazoncorretto:21 AS build

WORKDIR /app

RUN yum install -y tar gzip


COPY mvnw pom.xml ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 3002

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
