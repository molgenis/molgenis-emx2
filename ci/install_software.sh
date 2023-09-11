# install software needed for our build env
apt update
apt-get install -y gnupg lsb-release ca-certificates curl apt-transport-https
install -m 0755 -d /etc/apt/keyrings

# install postgresql client
apt-get install postgresql-client -y

# install node
curl -sL https://deb.nodesource.com/setup_18.x | bash -
apt install nodejs

# install playwright
npx playwright install --with-deps
npm install -D @playwright/test

# install python
apt-get -y install python3 python3-venv python3-pip
python3 -m pip install --upgrade build twine

# install docker
curl -sSL https://get.docker.com/ | sh

# install kubectl
curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add
echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee /etc/apt/sources.list.d/kubernetes.list
apt-get update
apt-get install kubectl -y

# install helm
curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | tee /usr/share/keyrings/helm.gpg > /dev/null
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | tee /etc/apt/sources.list.d/helm-stable-debian.list
apt-get update
apt-get install helm -y