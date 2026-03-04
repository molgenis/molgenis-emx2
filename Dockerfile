FROM ubuntu:24.04

RUN apt update && apt -y upgrade
RUN apt update && apt -y install python3 python3-pip python3-venv openjdk-21-jre-headless

ARG JAR_FILE
COPY build/libs/${JAR_FILE} app.jar
COPY custom-app custom-app
EXPOSE 8080
RUN useradd -m molgenis

USER molgenis
ENTRYPOINT ["java","-jar","app.jar"]
