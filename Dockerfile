# Build:
# docker build . -t emx2:latest
# Test run:
# docker run --name=test -p 8080:8080 --network="host" -e jdbc=jdbc:postgresql:localhost/molgenis -d emx2:latest
# Clean up:
# docker rm test
# Publish
# docker tag emx2:latest mswertz/emx2:latest
# docker push mswertz/emx2
FROM openjdk:13-alpine
ADD molgenis-emx2-webservice/target/molgenis-emx2-jar-with-dependencies.jar emx2.jar
EXPOSE 8080
CMD java -jar emx2.jar ${jdbc}
