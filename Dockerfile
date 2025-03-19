FROM eclipse-temurin:21-jre-noble
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
RUN apt-get update && apt-get install python3 python3-pip python3-venv python3-setuptools python3-setuptools-whl -y
RUN useradd -m molgenis
USER molgenis
ENTRYPOINT ["java","-jar","app.jar"]
