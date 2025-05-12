# docker file for the ci
# to build and publish (n.b. on apple silicon you need to set --platform)
# docker buildx build -t molgenis/ci-build:1.2.2 --platform linux/amd64 .
# docker push molgenis/ci-build:1.2.2

FROM gradle:jdk21-noble
RUN useradd -m molgenis

RUN apt-get update && apt-get install vim python3 python3-venv python3-pip python3-setuptools python3-setuptools-whl -y
RUN python3 -m pip config set global.break-system-packages true
RUN python3 --version

ENV NODE_VERSION=20.11.0
RUN apt install -y curl
USER molgenis 
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
ENV NVM_DIR=/home/molgenis/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/home/molgenis/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN node --version
RUN npm --version
USER root 


RUN apt update
RUN apt-get install -y gnupg lsb-release ca-certificates curl apt-transport-https
RUN install -m 0755 -d /etc/apt/keyrings

##docker
RUN curl -sSL https://get.docker.com/ | sh
RUN mkdir ~/.docker
RUN curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s 

# kubectl
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl
#
##helm repo
RUN curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | tee /usr/share/keyrings/helm.gpg > /dev/null
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | tee /etc/apt/sources.list.d/helm-stable-debian.list
RUN apt-get update
RUN apt-get install helm -y
#

# Azure CLI Tools
RUN mkdir -p /etc/apt/keyrings
RUN curl -sLS https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | tee /etc/apt/keyrings/microsoft.gpg > /dev/null
RUN chmod go+r /etc/apt/keyrings/microsoft.gpg
RUN  AZ_DIST=$(lsb_release -cs) && echo "deb [arch=`dpkg --print-architecture` signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $AZ_DIST main" | tee /etc/apt/sources.list.d/azure-cli.list
RUN apt-get update
RUN apt-get install azure-cli -y

# postgresl client
RUN apt-get install postgresql-client -y

# GemTools & FPM For debian and redhat packages
RUN apt-get install ruby-dev -y 
RUN gem install fpm
USER root 
