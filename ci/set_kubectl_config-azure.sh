echo " logging in to azure with service principal and get kube config"
az login --service-principal --tenant ${AZURE_SP_TENANT} -u ${AZURE_CLIENT_ID} -p ${AZURE_SECRET}
az aks get-credentials -g ${RESOURCE_GROUP} -n ${RESOURCE_GROUP}

kubectl config set-cluster ${RESOURCE_GROUP}
kubectl config use-context ${RESOURCE_GROUP}
