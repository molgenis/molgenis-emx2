# docker file for the ci
# to build and publish (n.b. on apple silicon you need to set --platform)
# docker buildx build -t molgenis/ci-build:1.3.5 --platform linux/amd64 .
# ua

FROM gradle:jdk21-noble
RUN useradd -m molgenis

RUN apt-get update && apt-get install vim python3 python3-venv python3-pip python3-setuptools python3-setuptools-whl -y
RUN python3 -m pip config set global.break-system-packages true
RUN python3 --version

ENV NODE_VERSION=22.17.0
ENV PNPM_VERSION=10.10.0
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
RUN npm install -g pnpm@v${PNPM_VERSION}
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

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    ca-certificates \
    gnupg \
    lsb-release \
    tar \
    gzip \
    && rm -rf /var/lib/apt/lists/*

# Set the Helm version you want
ARG HELM_VERSION=v3.12.0

# Download and install Helm
RUN curl -fsSL https://get.helm.sh/helm-${HELM_VERSION}-linux-amd64.tar.gz \
    | tar -xz -C /tmp \
    && mv /tmp/linux-amd64/helm /usr/local/bin/helm \
    && chmod +x /usr/local/bin/helm \
    && rm -rf /tmp/linux-amd64
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
USER molgenis
