FROM maven:3.9.6-amazoncorretto-21 as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled Java application JAR file into the container
COPY target/ewm-service-0.0.1-SNAPSHOT.jar /app/ewm-service-0.0.1-SNAPSHOT.jar

# Command to run the Java application
ENTRYPOINT ["java", "-jar", "ewm-service-0.0.1-SNAPSHOT.jar"]
