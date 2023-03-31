pipeline {
    agent {
        kubernetes {
            inheritFrom "shared"
            yamlFile ".jenkins/build-pod.yaml"
        }
    }
    environment {
        DOCKER_CONFIG = "/root/.docker"
        CHART_VERSION = "8.150.0"
        MOLGENIS_POSTGRES_USER = 'molgenis'
        MOLGENIS_POSTGRES_PASS = 'molgenis'
        MOLGENIS_POSTGRES_URI = 'jdbc:postgresql://localhost/molgenis'
    }
    stages {
        stage('Prepare') {
            when {
                anyOf {
                    allOf {
                        changeRequest()
                        branch 'PR-*'
                    }
                    branch 'master'
               }
            }
            steps {
                checkout scm
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.kube"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                        sh(script: "vault read -field=value secret/ops/jenkins/kube/config > ${JENKINS_AGENT_WORKDIR}/.kube/config")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=token-emx2 secret/ops/token/github', returnStdout: true)
                        env.DOCKERHUB_AUTH = sh(script: 'vault read -field=value secret/gcc/token/dockerhub', returnStdout: true)
                        env.NEXUS_USER = sh(script: 'vault read -field=username secret/ops/account/nexus', returnStdout: true)
                        env.NEXUS_PWD = sh(script: 'vault read -field=password secret/ops/account/nexus', returnStdout: true)
                    }
                }
                container("java") {
                    sh "git config --global --add safe.directory '*'"
                    sh 'git fetch --depth 100000'
                    sh "git config user.email \"molgenis@gmail.com\""
                    sh "git config user.name \"molgenis-jenkins\""
                    sh 'git config url.https://.insteadOf git://'
                    sh "mkdir -p ${DOCKER_CONFIG}"
                    sh "echo '{\"auths\": {\"https://index.docker.io/v1/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}, \"registry.hub.docker.com\": {\"auth\": \"${DOCKERHUB_AUTH}\"}}}' > ${DOCKER_CONFIG}/config.json"
                    sh "apt-get update && apt-get install postgresql-client -y"
                    sh "psql -h 127.0.0.1 -p 5432 -U postgres < .docker/initdb.sql"
                }
                dir("${JENKINS_AGENT_WORKDIR}/.m2") {
                    stash includes: 'settings.xml', name: 'maven-settings'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
                    stash includes: 'cli2.json', name: 'rancher-config'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.kube") {
                    stash includes: 'config', name: 'kube-config'
                }
            }
        }
        stage("Pull request") {
            when {
                allOf {
                    changeRequest()
                    branch 'PR-*'
               }
            }
            environment {
                NAME = "preview-emx2-pr-${CHANGE_ID.toLowerCase()}"
            }
            steps {
                container('java') {
                    script {
                    sh "./gradlew test --no-daemon jacocoMergedReport shadowJar jib release helmPublishMainChart ci \
                        -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                        -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"
                        def props = readProperties file: 'build/ci.properties'
                        env.TAG_NAME = props.tagName
                        sh "./gradlew --no-daemon helmLintMainChart --info"
                    }
                }
                container (name: 'kaniko', shell: '/busybox/sh') {
                    sh "#!/busybox/sh\nmkdir -p ${DOCKER_CONFIG}"
                    sh "#!/busybox/sh\necho '{\"auths\": {\"https://index.docker.io/v1/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}, \"registry.hub.docker.com/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}}}' > ${DOCKER_CONFIG}/config.json"
                    sh "#!/busybox/sh\n/kaniko/executor --dockerfile ${WORKSPACE}/apps/nuxt3-ssr/Dockerfile --context ${WORKSPACE}/apps/nuxt3-ssr --destination docker.io/molgenis/ssr-catalogue-snapshot:${TAG_NAME} --destination docker.io/molgenis/ssr-catalogue-snapshot:latest"
                }
                container('helm') {
                    sh "kubectl delete namespace ${NAME} || true"
                    sh "sleep 15s" // wait for deletion
                    sh "kubectl create namespace ${NAME}"
                    sh "kubectl annotate --overwrite ns ${NAME} field.cattle.io/projectId=\"c-l4svj:p-tl227\""
                    sh "helm install ${NAME} ./helm-chart --namespace ${NAME} " +
                        "--set ingress.hosts[0].host=${NAME}.dev.molgenis.org " +
                        "--set adminPassword=admin " +
                        "--set image.tag=${TAG_NAME} " +
                        "--set image.repository=molgenis/molgenis-emx2-snapshot " +
                        "--set image.pullPolicy=Always " +
                        "--set ingress.hosts[0].host=${NAME}.dev.molgenis.org " +
                        "--set ssrCatalogue.image.tag=${TAG_NAME} " +
                        "--set ssrCatalogue.environment.siteTitle=\"Preview Catalogue\" " +                 
                        "--set ssrCatalogue.environment.apiBase=https://${NAME}.dev.molgenis.org/ " +
                        "--set catalogue.includeCatalogueDemo=true "

                }
            }
            post {
                success {
                    molgenisSlack(message: "PR Preview available on https://${NAME}.dev.molgenis.org", status:'INFO', channel: '#pr-emx2')
                }
            }
        }
        stage('Master') {
            when {
                branch 'master'
            }
            steps {
                container('java') {
                    script {
                        sh "./gradlew test --no-daemon jacocoMergedReport shadowJar jib release helmPublishMainChart sonarqube ci \
                            -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                            -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"
                        def props = readProperties file: 'build/ci.properties'
                        env.TAG_NAME = props.tagName
                    }
                }
                container (name: 'kaniko', shell: '/busybox/sh') {
                    sh "#!/busybox/sh\nmkdir -p ${DOCKER_CONFIG}"
                    sh "#!/busybox/sh\necho '{\"auths\": {\"https://index.docker.io/v1/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}, \"registry.hub.docker.com/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}}}' > ${DOCKER_CONFIG}/config.json"
                    sh "#!/busybox/sh\n/kaniko/executor --dockerfile ${WORKSPACE}/apps/nuxt3-ssr/Dockerfile  --context ${WORKSPACE}/apps/nuxt3-ssr --destination docker.io/molgenis/ssr-catalogue:${TAG_NAME} --destination docker.io/molgenis/ssr-catalogue:latest"
                }
                container('rancher') {
                    script {
                        sh 'rancher context switch dev-molgenis'
                        sh "sleep 30s" // wait for chart publish
                        env.REPOSITORY = env.TAG_NAME.toString().contains('-SNAPSHOT') ? 'molgenis/molgenis-emx2-snapshot' : 'molgenis/molgenis-emx2'
                        sh "rancher apps upgrade --set image.tag=${TAG_NAME} --set image.repository=${REPOSITORY} p-tl227:emx2 ${CHART_VERSION}"
                    }
                }
            }
            post {
                success {
                    molgenisSlack(message: "EMX2 version: ${TAG_NAME} is released. Check it out: https://emx2.dev.molgenis.org", status:'INFO', channel: '#pr-emx2')
                }
            }
        }
        
    }
}
