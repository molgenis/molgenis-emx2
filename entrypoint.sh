#!/bin/sh
if [ -z "$JAVA_OPTS" ]; then
    exec java -jar app.jar
else
    exec java $JAVA_OPTS -jar app.jar
fi