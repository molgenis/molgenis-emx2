FROM eclipse-temurin:17.0.10_7-jdk-focal
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
RUN apt-get update
RUN apt-get install software-properties-common -y
RUN apt-get update && add-apt-repository ppa:deadsnakes/ppa
RUN apt-get update && apt-get install python3.12 python3-venv -y
RUN update-alternatives --install /usr/bin/python3 python /usr/bin/python3.12 1
RUN apt-get update && apt install python-is-python3
RUN apt-get update && apt-get install python3.12-dev python3.12-venv -y
RUN apt-get update && apt install python3.12-distutils
ENTRYPOINT ["java","-jar","app.jar"]
