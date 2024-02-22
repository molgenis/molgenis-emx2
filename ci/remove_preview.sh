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

