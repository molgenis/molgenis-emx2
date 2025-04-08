FROM ubuntu:24.10

RUN apt update && apt -y upgrade
RUN apt update && apt -y install python3 python3-pip python3-venv openjdk-21-jre-headless

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
RUN useradd -m molgenis

USER molgenis
ENTRYPOINT ["java","-jar","app.jar"]
