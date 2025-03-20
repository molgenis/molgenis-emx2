#FROM eclipse-temurin:21-jre-noble
FROM ubuntu:24.10
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
RUN apt-get update && apt-get install openjdk-21-jre-headless python3 python3-pip python3-venv -y
RUN pip3 install setuptools --break-system-packages
RUN useradd -m molgenis
USER molgenis
ENTRYPOINT ["java","-jar","app.jar"]
