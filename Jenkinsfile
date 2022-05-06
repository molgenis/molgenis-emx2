pipeline {
    agent {
        kubernetes {
            inheritFrom "shared"
            yamlFile ".jenkins/build-pod.yaml"
        }
    }
    environment {
        DOCKER_CONFIG = "/root/.docker"
        CHART_VERSION = "8.73.1"
        MOLGENIS_POSTGRES_USER = 'molgenis_admin'
        MOLGENIS_POSTGRES_PASS = 'molgenis_admin'
        MOLGENIS_POSTGRES_URI = 'jdbc:postgresql://localhost/molgenisdb'
    }
    stages {
        stage('Prepare') {
            steps {
                checkout scm
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=token-emx2 secret/ops/token/github', returnStdout: true)
                        env.DOCKERHUB_AUTH = sh(script: 'vault read -field=value secret/gcc/token/dockerhub', returnStdout: true)
                        env.NEXUS_USER = sh(script: 'vault read -field=username secret/ops/account/nexus', returnStdout: true)
                        env.NEXUS_PWD = sh(script: 'vault read -field=password secret/ops/account/nexus', returnStdout: true)
                    }
                }
                container("java") {
                    sh 'apt update'
                    sh 'apt -y install docker.io'
                    sh 'git fetch --depth 10000'
                    sh "git config user.email \"molgenis@gmail.com\""
                    sh "git config user.name \"molgenis-jenkins\""
                    sh 'git config url.https://.insteadOf git://'
                    sh "mkdir -p ${DOCKER_CONFIG}"
                    sh "echo '{\"auths\": {\"https://index.docker.io/v1/\": {\"auth\": \"${DOCKERHUB_AUTH}\"}, \"registry.hub.docker.com\": {\"auth\": \"${DOCKERHUB_AUTH}\"}}}' > ${DOCKER_CONFIG}/config.json"
                }
                dir("${JENKINS_AGENT_WORKDIR}/.m2") {
                    stash includes: 'settings.xml', name: 'maven-settings'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
                    stash includes: 'cli2.json', name: 'rancher-config'
                }
            }
        }
        stage("Pull request") {
            when {
                changeRequest()
            }
            environment {
                NAME = "preview-emx2-pr-${CHANGE_ID.toLowerCase()}"
            }
            steps {
                container('java') {
                    script {
                    sh "./gradlew test jacocoMergedReport shadowJar jib release ci \
                        -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                        -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"
                        def props = readProperties file: 'build/ci.properties'
                        env.TAG_NAME = props.tagName
                        sh "./gradlew helmLintMainChart --info"
                    }
                }
                container('rancher') {
                    sh "rancher apps delete ${NAME} || true"
                    sh "sleep 15s" // wait for deletion
                    sh "rancher apps install " + 
                        "-n ${NAME} " +
                        "p-vx5vf:molgenis-helm3-emx2 " +
                        "${NAME} " +
                        "--no-prompt " +
                        "--set adminPassword=admin " +
                        "--set image.tag=${TAG_NAME} " +
                        "--set image.repository=molgenis/molgenis-emx2-snapshot " +
                        "--set image.pullPolicy=Always " +
                        "--set ingress.hosts[0].host=${NAME}.dev.molgenis.org"
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
                allOf {
                    branch 'master'
                }
            }
            steps {
                container('java') {
                    script {
                        sh "./gradlew test jacocoMergedReport shadowJar jib release helmPublishMainChart sonarqube ci \
                            -Dsonar.login=${SONAR_TOKEN} -Dsonar.organization=molgenis -Dsonar.host.url=https://sonarcloud.io \
                            -Dorg.ajoberstar.grgit.auth.username=${GITHUB_TOKEN} -Dorg.ajoberstar.grgit.auth.password"
                        def props = readProperties file: 'build/ci.properties'
                        env.TAG_NAME = props.tagName
                    }
                }
                container('rancher') {
                    script {
                        sh 'rancher context switch dev-molgenis'
                        env.REPOSITORY = env.TAG_NAME.toString().contains('-SNAPSHOT') ? 'molgenis/molgenis-emx2-snapshot' : 'molgenis/molgenis-emx2'
                        sh "rancher apps upgrade --set image.tag=${TAG_NAME} --set image.repository=${REPOSITORY} molgenis-emx2 ${CHART_VERSION}"
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
