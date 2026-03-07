# --- Build stage: extract JAR into layers ---
FROM eclipse-temurin:21-jdk-noble AS builder
ARG JAR_FILE
COPY build/libs/${JAR_FILE} /tmp/app.jar
RUN mkdir -p /app/deps /app/classes /app/frontend && \
    cd /app/deps && jar -xf /tmp/app.jar && \
    mkdir -p /app/classes/org && \
    mv /app/deps/org/molgenis /app/classes/org/molgenis && \
    mv /app/deps/public_html /app/frontend/public_html && \
    rm -f /app/deps/META-INF/*.SF /app/deps/META-INF/*.DSA /app/deps/META-INF/*.RSA && \
    rm /tmp/app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-noble

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis

COPY --link --from=builder /app/deps /app
COPY --link --from=builder /app/frontend/public_html /app/public_html
COPY --link --from=builder /app/classes/org/molgenis /app/org/molgenis
COPY --link custom-app /custom-app
COPY --link entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER molgenis
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
