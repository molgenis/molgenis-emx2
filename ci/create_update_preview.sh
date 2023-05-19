#this scripts two arguments
# $1 = ${NAME} meaning name of the namespace on the server
# $2 = and ${TAG_NAME} meaning the docker image tag name

NAME=$1
TAG_NAME=$2

echo $NAME
echo $TAG_NAME

#assuming ubuntu, install kubcetl and helm

#delete if exists
kubectl delete namespace $NAME || true
# wait for deletion to complete
sleep 15s
kubectl create namespace $NAME
kubectl annotate --overwrite ns $NAME field.cattle.io/projectId=\"c-l4svj:p-tl227\"
helm install $NAME ./helm-chart --namespace $NAME \
--set ingress.hosts[0].host=$NAME.dev.molgenis.org \
--set adminPassword=admin \
--set image.tag=${TAG_NAME} \
--set image.repository=molgenis/molgenis-emx2-snapshot \
--set image.pullPolicy=Always \
--set ingress.hosts[0].host=$NAME.dev.molgenis.org \
--set ssrCatalogue.image.tag=$TAG_NAME \
--set ssrCatalogue.environment.siteTitle=\"Preview Catalogue\" \
--set ssrCatalogue.environment.apiBase=https://$NAME.dev.molgenis.org/ \
--set catalogue.includeCatalogueDemo=true