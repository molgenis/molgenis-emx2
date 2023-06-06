#this scripts takes arguments
NAME=$1
TAG_NAME=$2
DELETE=$3

echo "Using namespace $NAME"
echo "Using docker tag_name $TAG_NAME"
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
# wait for deletion to complete
sleep 15s
kubectl create namespace $NAME
kubectl annotate --overwrite ns $NAME field.cattle.io/projectId="c-l4svj:p-tl227"
helm upgrade --install ${NAME} ./helm-chart --namespace ${NAME} \
--set ingress.hosts[0].host=${NAME}.dev.molgenis.org \
--set adminPassword=admin \
--set image.tag=${TAG_NAME} \
--set image.repository=${REPO} \
--set image.pullPolicy=Always \
--set catalogue.includeCatalogueDemo=true \
--set ssrCatalogue.image.tag=$TAG_NAME \
--set ssrCatalogue.image.repository=$REPO2 \
--set ssrCatalogue.environment.siteTitle="Preview Catalogue" \
--set ssrCatalogue.environment.apiBase=https://${NAME}.dev.molgenis.org/ \
--set catalogue.includeCatalogueDemo=true
