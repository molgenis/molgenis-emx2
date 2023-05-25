FROM eclipse-temurin:17.0.7_7-jdk-focal
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
RUN apt-get update && apt-get install python3 python3-venv -y
ENTRYPOINT ["java","-jar","app.jar"]
