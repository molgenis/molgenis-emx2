#this scripts takes arguments
NAME=$1
TAG_NAME=$2
DELETE=$3

CATALOGUE="${NAME/emx2/catalogue}"

echo "Using namespace $NAME"
echo "Using docker tag_name $TAG_NAME"
echo "Using catalogue name $CATALOGUE"
echo "Delete=$DELETE"

REPO=molgenis/molgenis-emx2
REPO2=molgenis/ssr-catalogue
if [[ $TAG_NAME == *"SNAPSHOT"* ]]; then
  REPO=molgenis/molgenis-emx2-snapshot
  REPO2=molgenis/ssr-catalogue-snapshot
fi
echo "Using repositories $REPO and $REPO2"

# delete if exists
if [ ! -z "$DELETE" ]
then
  kubectl delete namespace $NAME || true
fi
# Create certs from environement
echo ${CERTDEVMOLGENIS_KEY} | base64 --decode >> /tmp/cert_key 
echo ${CERTDEVMOLGENIS_PEM} | base64 --decode >> /tmp/cert_pem

# wait for deletion to complete
sleep 15s
kubectl create namespace $NAME
kubectl create secret tls "dev.molgenis.org" --key /tmp/cert_key --cert /tmp/cert_pem -n ${NAME}

helm upgrade --install ${NAME} ./helm-chart --namespace ${NAME} \
--set ingress.hosts[0].host=${NAME}.dev.molgenis.org \
--set spec.tls[0].hosts[0].host=${NAME}.dev.molgenis.org \
--set ingress.hosts[0].host=${CATALOGUE}.dev.molgenis.org \
--set spec.tls[0].hosts[0].host=${CATALOGUE}.dev.molgenis.org \
--set adminPassword=admin \
--set image.tag=${TAG_NAME} \
--set image.repository=${REPO} \
--set image.pullPolicy=Always \
--set ssrCatalogue.image.tag=$TAG_NAME \
--set ssrCatalogue.image.repository=$REPO2 \
--set ssrCatalogue.environment.siteTitle="Preview Catalogue" \
--set ssrCatalogue.environment.apiBase=https://${NAME}.dev.molgenis.org/ \
--set includeTypeTestDemo=true \
--set catalogue.includeCatalogueDemo=true \
--set directory.includeDirectoryDemo=true \
--set includePatientRegistryDemo=true \
--set oidc.enabled=true \
--set oidc.client_id=${OIDC_CLIENTID} \
--set oidc.client_secret=${OIDC_SECRET} \
--set oidc.client_name=${NAME} \
--set oidc.discovery_url=${OIDC_DISCOVERYURL} \
--set oidc.callback_url=https://${NAME}.dev.molgenis.org


rm /tmp/cert_key
rm /tmp/cert_pem

