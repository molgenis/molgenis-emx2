#this scripts takes arguments
NAME=$1
TAG_NAME=$2
KUBE_CLUSTER=$3
KUBE_TOKEN=$4

echo "Using namespace $NAME"
echo "Using docker tagname $TAG_NAME"
echo "Using kube_cluster $KUBE_CLUSTER"
echo "Using kube_token $KUBE_TOKEN"

#assuming ubuntu, install kubcetl and helm

#create config
kubectl config set-cluster molgenis-dev --server=$KUBE_CLUSTER
kubectl config set-credentials molgenis-dev --token=$KUBE_TOKEN
kubectl config set-context molgenis-dev --cluster=molgenis-dev --user=molgenis-dev
kubectl config use-context molgenis-dev
kubectl config view

# delete if exists
kubectl delete namespace $NAME || true
# wait for deletion to complete
sleep 15s
kubectl create namespace $NAME
kubectl annotate --overwrite ns $NAME field.cattle.io/projectId="c-l4svj:p-tl227"
helm install $NAME ./helm-chart --namespace $NAME \
--set ingress.hosts[0].host=$NAME.dev.molgenis.org \
--set adminPassword=admin \
--set image.tag=${TAG_NAME} \
--set image.repository=molgenis/molgenis-emx2-snapshot \
--set image.pullPolicy=Always \
--set catalogue.includeCatalogueDemo=true
#--set ssrCatalogue.image.tag=$TAG_NAME \
#--set ssrCatalogue.environment.siteTitle="Preview Catalogue" \
#--set ssrCatalogue.environment.apiBase=https://$NAME.dev.molgenis.org/