FROM eclipse-temurin:21-jre-noble

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis

RUN python3 -m pip config set global.break-system-packages true && pip install uv

COPY --link build/docker/deps/ /app/lib/
COPY --link build/docker/app/ /app/lib/
COPY --link custom-app /app/lib/custom-app

ENV CUSTOM_APP_PATH="/app/lib/custom-app"

USER molgenis
EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-cp", "/app/lib/*", "org.molgenis.emx2.RunMolgenisEmx2"]
