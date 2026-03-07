FROM eclipse-temurin:21-jre-noble

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis

ARG JAR_FILE
COPY --link build/libs/${JAR_FILE} /app/app.jar
COPY --link custom-app /custom-app

USER molgenis
EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-jar", "/app/app.jar"]
