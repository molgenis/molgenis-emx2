FROM bitnami/kubectl:1.20.9 as kubectl
FROM gradle:jdk17-jammy

RUN apt-get update && apt-get install python3 python3-venv -y
RUN apt-get install python3 python3-venv python3-pip -y
RUN python3 -m pip install --upgrade build twine
RUN python3 --version

ENV NODE_VERSION=20.11.0
RUN apt install -y curl
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN node --version
RUN npm --version


RUN apt update
RUN apt-get install -y gnupg lsb-release ca-certificates curl apt-transport-https
RUN install -m 0755 -d /etc/apt/keyrings

##docker
RUN curl -sSL https://get.docker.com/ | sh

# kubectl repo
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl /usr/local/bin/

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
RUN apt-get install azure-cli