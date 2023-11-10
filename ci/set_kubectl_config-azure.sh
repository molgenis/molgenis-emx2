KUBE_CLUSTER=$1
KUBE_TOKEN=$2

echo " logging in to azure with service principal and get kube config"
az login --service-principal --tenant ${AZURE_SP_TENANT} -u ${AZURE_SP} -p ${AZURE_SP_PASSWORD}
az aks get-credentials -g ${RESOURCE_GROUP} -n ${RESOURCE_GROUP}

kubectl config set-cluster ${RESOURCE_GROUP}
kubectl config use-context ${RESOURCE_GROUP}
