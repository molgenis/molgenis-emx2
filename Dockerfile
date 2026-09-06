FROM eclipse-temurin:21-jre-noble

RUN apt update && \
    apt install -y --no-install-recommends python3 pipx && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis


COPY --link build/docker/deps/ /app/lib/
COPY --link build/docker/app/ /app/lib/
COPY --link custom-app /app/lib/custom-app

ENV CUSTOM_APP_PATH="/app/lib/custom-app"

USER molgenis
RUN pipx ensurepath && pipx install uv
RUN pipx run uv --version
EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-cp", "/app/lib/*", "org.molgenis.emx2.RunMolgenisEmx2"]
