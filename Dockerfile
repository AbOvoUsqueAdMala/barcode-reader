FROM openjdk:21-bullseye
RUN apt update && apt install -y curl
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8000:8080
ENTRYPOINT ["java", "-jar", "/app.jar"]