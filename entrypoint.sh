#!/bin/sh
if [ -z "$JAVA_OPTS" ]; then
    exec java -cp "/app" org.molgenis.emx2.RunMolgenisEmx2
else
    exec java $JAVA_OPTS -cp "/app" org.molgenis.emx2.RunMolgenisEmx2
fi
