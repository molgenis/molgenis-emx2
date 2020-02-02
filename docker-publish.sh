#!/bin/bash

# publish docker file to docker hub mswertz/emx2:latest
#echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
#docker build . -t emx2:latest
#docker tag emx2:latest mswertz/emx2:latest
#docker push mswertz/emx2

# create helm chart in docs/helm
helm lint helm-chart
helm package helm-chart -d docs/helm-charts/.
helm package helm-chart -d docs/helm-charts/.
helm repo index docs/helm-charts --url http://mswertz.github.io/molgenis-emx2/helm-charts/
