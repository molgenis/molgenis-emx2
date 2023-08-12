# install software needed for our build env
apt update
apt-get install -y gnupg lsb-release ca-certificates curl apt-transport-https
install -m 0755 -d /etc/apt/keyrings

##docker
curl -sSL https://get.docker.com/ | sh

# kubectl repo
curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add
echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee /etc/apt/sources.list.d/kubernetes.list
apt-get update
apt-get install kubectl -y

#
##helm repo
curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | tee /usr/share/keyrings/helm.gpg > /dev/null
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | tee /etc/apt/sources.list.d/helm-stable-debian.list
apt-get update
apt-get install helm -y
#

