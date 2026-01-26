FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
VOLUME /tmp
ENTRYPOINT ["java", "-jar", "app.jar"]