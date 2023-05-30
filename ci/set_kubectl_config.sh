KUBE_CLUSTER=$1
KUBE_TOKEN=$2

echo "Using kube_cluster $KUBE_CLUSTER"
echo "Using kube_token $KUBE_TOKEN"

#create config
kubectl config set-cluster molgenis-dev --server=$KUBE_CLUSTER
kubectl config set-context molgenis-dev --cluster=molgenis-dev --user=molgenis-dev
kubectl config use-context molgenis-dev
kubectl config set-credentials molgenis-dev --token=$KUBE_TOKEN
kubectl config view