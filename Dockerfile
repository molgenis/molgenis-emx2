FROM eclipse-temurin:21-jre-noble

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis

COPY --link build/docker/deps/ /app/lib/
COPY --link build/docker/app/ /app/lib/
COPY --link custom-app /app/lib/custom-app

USER molgenis
EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-cp", "/app/lib/*", "org.molgenis.emx2.RunMolgenisEmx2"]
