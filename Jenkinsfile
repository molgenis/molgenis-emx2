pipeline {
    agent {
        kubernetes {
            label 'molgenis-jdk11'
        }
    }
    environment {
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stages {
        stage('Retrieve build secrets') {
            steps {
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.m2"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=value secret/ops/jenkins/maven/settings.xml > ${JENKINS_AGENT_WORKDIR}/.m2/settings.xml")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                        env.DOCKER_USERNAME = sh(script: 'vault read -field=username secret/gcc/account/dockerhub', returnStdout: true)
                        env.DOCKER_PASSWORD = sh(script: 'vault read -field=password secret/gcc/account/dockerhub > ~/docker_password.txt', returnStdout: true)

                    }
                }
                dir("${JENKINS_AGENT_WORKDIR}/.m2") {
                    stash includes: 'settings.xml', name: 'maven-settings'
                }
                dir("${JENKINS_AGENT_WORKDIR}/.rancher") {
                    stash includes: 'cli2.json', name: 'rancher-config'
                }
            }
        }
        stage('Steps [ master ]') {
            when {
                branch 'master'
            }
            stages {
                stage('Build, Test [ master ]') {
                    steps {
                        container('maven') {
                            script {
                                sh "cat ~/docker_password.txt | docker login -u \"$DOCKER_USERNAME\" --password-stdin"
                                docker.image('postgres:13-alpine').withRun('-p 5432:5432 -P --name postgres -e "POSTGRES_DB=molgenis" -e "POSTGRES_USER=molgenis" -e "POSTGRES_PASSWORD=molgenis"') { postgres ->
                                    docker.image('postgres:13-alpine').inside("--link ${postgres.id}:postgres") {
                                         sh 'sleep 15'
                                    }
                                    docker.image("maven").inside("--link ${postgres.id}:postgres") {
                                        sh "./gradlew test -DMOLGENIS_POSTGRES_URI=jdbc:postgresql://postgres/molgenis"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
